package ru.dargen.modulo.module;

public class ModuleException extends RuntimeException {

    public ModuleException(String name, String message, Throwable cause) {
        super("%s: %s".formatted(message, name), cause);
    }

    public ModuleException(String name, String message) {
        this(name, message, null);
    }

}
