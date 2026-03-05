#!/usr/bin/env bash
set -euo pipefail

EXAMPLES_DIR="target/mapstruct-examples"
PROCESSOR_VERSION="${QUARKUS_MAPSTRUCT_VERSION:-999-SNAPSHOT}"

# Clone mapstruct-examples if not already present
if [ ! -d "$EXAMPLES_DIR" ]; then
  git clone https://github.com/mapstruct/mapstruct-examples "$EXAMPLES_DIR"
fi

# Replace mapstruct-processor with quarkus-mapstruct-processor in all pom.xml files
java ReplaceProcessor.java "$EXAMPLES_DIR" "$PROCESSOR_VERSION"

cd "$EXAMPLES_DIR"

mvn clean install
