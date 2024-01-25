package ru.dargen.modulo.module;

public interface ModuleEntrypoint {

    default void enable() {}

    default void disable() {}

    default void reload() {}

}
