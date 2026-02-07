package io.quarkiverse.mapstruct.it.configmapping;

import java.util.List;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "parent")
public interface Parent {
    String prop();

    List<Child1> childs();
}
