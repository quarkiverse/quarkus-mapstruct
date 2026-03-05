import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Replaces mapstruct-processor with quarkus-mapstruct-processor in all pom.xml files
 * found under a given directory (recursively).
 *
 * Usage: java ReplaceProcessor.java [examples-dir] [quarkus-mapstruct-processor-version]
 *   examples-dir: path to mapstruct-examples clone (default: target/mapstruct-examples)
 *   version:      quarkus-mapstruct-processor version to use (default: 999-SNAPSHOT)
 */
public class ReplaceProcessor {

    public static void main(String[] args) throws IOException {
        Path rootDir = args.length > 0 ? Path.of(args[0]) : Path.of("target/mapstruct-examples");
        String processorVersion = args.length > 1 ? args[1] : "999-SNAPSHOT";

        if (!Files.isDirectory(rootDir)) {
            System.err.println("Directory not found: " + rootDir);
            System.exit(1);
        }

        System.out.println("Scanning: " + rootDir.toAbsolutePath());
        System.out.println("Replacing mapstruct-processor with quarkus-mapstruct-processor:" + processorVersion);
        System.out.println();

        int[] count = {0};
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(p -> p.getFileName().toString().equals("pom.xml"))
                  .forEach(pom -> {
                      if (replaceProcessor(pom, processorVersion)) {
                          count[0]++;
                      }
                  });
        }

        System.out.println();
        System.out.println("Done. Modified " + count[0] + " pom.xml file(s).");
    }

    private static boolean replaceProcessor(Path pom, String version) {
        try {
            String original = Files.readString(pom);
            String modified = replaceProcessorInContent(original, version);
            if (original.equals(modified)) {
                return false;
            }
            Files.writeString(pom, modified);
            System.out.println("  Modified: " + pom);
            return true;
        } catch (IOException e) {
            System.err.println("  Error processing " + pom + ": " + e.getMessage());
            return false;
        }
    }

    static String replaceProcessorInContent(String content, String version) {
        // Step 1: replace the artifactId — unambiguous, no other artifact uses this name
        String result = content.replace(
            "<artifactId>mapstruct-processor</artifactId>",
            "<artifactId>quarkus-mapstruct-processor</artifactId>"
        );

        if (result.equals(content)) {
            return content; // nothing to do
        }

        // Step 2: fix groupId adjacent to the replaced artifactId (handles both element orders)
        result = Pattern.compile(
            "<groupId>org\\.mapstruct</groupId>(\\s*<artifactId>quarkus-mapstruct-processor</artifactId>)",
            Pattern.DOTALL
        ).matcher(result).replaceAll("<groupId>io.quarkiverse.mapstruct</groupId>$1");

        result = Pattern.compile(
            "(<artifactId>quarkus-mapstruct-processor</artifactId>\\s*)<groupId>org\\.mapstruct</groupId>",
            Pattern.DOTALL
        ).matcher(result).replaceAll("$1<groupId>io.quarkiverse.mapstruct</groupId>");

        // Step 3: fix version adjacent to the replaced block (groupId-first or artifactId-first)
        result = Pattern.compile(
            "(<groupId>io\\.quarkiverse\\.mapstruct</groupId>\\s*" +
             "<artifactId>quarkus-mapstruct-processor</artifactId>\\s*" +
             "<version>)[^<]*(</version>)",
            Pattern.DOTALL
        ).matcher(result).replaceAll("$1" + version + "$2");

        result = Pattern.compile(
            "(<artifactId>quarkus-mapstruct-processor</artifactId>\\s*" +
             "<groupId>io\\.quarkiverse\\.mapstruct</groupId>\\s*" +
             "<version>)[^<]*(</version>)",
            Pattern.DOTALL
        ).matcher(result).replaceAll("$1" + version + "$2");

        return result;
    }
}
