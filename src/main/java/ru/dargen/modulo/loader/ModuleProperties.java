package ru.dargen.modulo.loader;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Properties;

import static java.lang.Boolean.parseBoolean;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class ModuleProperties {

    private final String name, entrypoint;
    private final boolean kotlinObjectEntryPoint;

    private final Properties rawProperties;

    public ModuleProperties(Properties properties) {
        this(
                properties.getProperty("name"), properties.getProperty("entrypoint"),
                parseBoolean(properties.getProperty("kotlin-object-entrypoint", "false")),
                properties
        );
    }

    public boolean equalsBase(ModuleProperties properties) {
        return properties.getName().equals(getName()) && properties.getEntrypoint().equals(getEntrypoint());
    }

    public void validate() {
        if (name == null || name.isEmpty() || name.isBlank()) {
            throw new IllegalArgumentException("Invalid module name property");
        }
        if (entrypoint == null || entrypoint.isEmpty() || entrypoint.isBlank()) {
            throw new IllegalArgumentException("Invalid module entrypoint property");
        }
    }

}
