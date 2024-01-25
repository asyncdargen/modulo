package ru.dargen.modulo.loader.exception;

public class ModuleLoadException extends RuntimeException {

    public ModuleLoadException(String sourceName, String message, Throwable cause) {
        super("%s: %s".formatted(message, sourceName), cause);
    }

    public ModuleLoadException(String sourceName, String message) {
        this(sourceName, message, null);
    }

}
