package io.quarkiverse.mapstruct.it.configmapping;

import java.util.List;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(beanStyleGetters = true)
public interface Child1 {
    String prop();

    List<Child2> childs();
}
