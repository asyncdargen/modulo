package ru.dargen.modulo.loader;

import java.util.Map;

public record ModuleRawInfo(ModuleProperties properties, Map<String, byte[]> entries) {

    public String getName() {
        return properties.getName();
    }

}
