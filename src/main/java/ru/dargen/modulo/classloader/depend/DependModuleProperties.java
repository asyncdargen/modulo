package ru.dargen.modulo.classloader.depend;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.lang.Boolean.parseBoolean;

public record DependModuleProperties(String moduleName,
                                     boolean isolated, boolean forceDepend, List<String> depends) {

    public DependModuleProperties(String moduleName, Properties properties) {
        this(
                moduleName,
                parseBoolean(properties.getProperty("isolated", "false")),
                parseBoolean(properties.getProperty("force-depend", "false")),
                Arrays.stream(properties.getProperty("depends", "").split("[, ]")).toList()
        );
    }

    public boolean hasAccessTo(DependModuleProperties properties) {
        return (!properties.isolated() && !isolated())
                || (properties.forceDepend() || forceDepend())
                || depends().contains(properties.moduleName());
    }

}
