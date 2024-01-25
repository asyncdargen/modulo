package ru.dargen.modulo.loader.exception;

public class ModuleConstructException extends RuntimeException {

    public ModuleConstructException(String name, String message, Throwable cause) {
        super("%s: %s".formatted(message, name), cause);
    }

    public ModuleConstructException(String name, String message) {
        this(name, message, null);
    }

}
