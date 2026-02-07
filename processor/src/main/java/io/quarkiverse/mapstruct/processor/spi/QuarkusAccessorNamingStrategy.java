package io.quarkiverse.mapstruct.processor.spi;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.spi.DefaultAccessorNamingStrategy;

/**
 * AccessorNamingStrategy which tells mapstruct how to deal with ConfigMap like interfaces.
 */
public class QuarkusAccessorNamingStrategy extends DefaultAccessorNamingStrategy {

    // TODO: bean style https://github.com/smallrye/smallrye-config/pull/1239/changes#diff-4841b7c8b98472d1ee27bdb4ca04e63f8f793e7c5d7f6089dc06b0bcd1e9568d

    @Override
    public boolean isGetterMethod(ExecutableElement method) {
        if (!isConfigMappingType(method)) {
            return super.isGetterMethod(method);
        }

        if (method.isDefault()) {
            return false;
        }

        // For ConfigMapping interfaces, all no-arg methods are getters
        return method.getParameters().isEmpty();
    }

    @Override
    public String getPropertyName(ExecutableElement method) {
        if (!isConfigMappingType(method)) {
            return super.getPropertyName(method);
        }

        // For ConfigMapping interfaces, the method name IS the property name (e.g., path() -> path)
        return method.getSimpleName().toString();
    }

    @Override
    public boolean isSetterMethod(ExecutableElement method) {
        if (!isConfigMappingType(method)) {
            return super.isSetterMethod(method);
        }

        return false;
    }

    @Override
    protected boolean isFluentSetter(ExecutableElement method) {
        if (!isConfigMappingType(method)) {
            return super.isFluentSetter(method);
        }

        return false;
    }

    private boolean isConfigMappingType(ExecutableElement method) {
        if (method.getEnclosingElement().getKind() != ElementKind.INTERFACE) {
            return false;
        }
        TypeElement enclosing = (TypeElement) method.getEnclosingElement();
        return ConfigMappingTypes.get()
                .contains(enclosing.getQualifiedName().toString());
    }
}
