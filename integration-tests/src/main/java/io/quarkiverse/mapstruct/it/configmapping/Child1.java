package io.quarkiverse.mapstruct.it.configmapping;

import io.smallrye.config.ConfigMapping;

import java.util.List;

@ConfigMapping(beanStyleGetters = true)
public interface Child1 {
    String prop();

    List<Child2> childs();
}
