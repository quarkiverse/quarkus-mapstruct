package io.quarkiverse.mapstruct.it.configmapping;

import java.util.List;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "parent", beanStyleGetters = true)
public interface ParentBeanStyle {
    String getProp();

    List<Child1> getChilds();
}
