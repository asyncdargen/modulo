package ru.dargen.modulo;

import ru.dargen.modulo.classloader.ModuleClassLoaderFactory;
import ru.dargen.modulo.loader.ModuleRawInfo;
import ru.dargen.modulo.module.Module;
import ru.dargen.modulo.module.ModuleException;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public interface Modulo {

    Logger LOGGER = Logger.getLogger("Modulo");

    Map<String, Module> getLoadedModules();

    ModuleClassLoaderFactory<?> getClassLoaderFactory();

    void lazilyEnabling(boolean lazilyEnabling);

    boolean isLazilyEnabling();

    List<Module> enableLazies(boolean disableLazily);

    default List<Module> enableLazies() {
        return enableLazies(false);
    }

    boolean isLoaded(String name);

    Module getModule(String name);

    Module loadModule(ModuleRawInfo info) throws ModuleException;

    boolean reloadModule(ModuleRawInfo info, boolean total) throws ModuleException;

    default boolean reloadModule(ModuleRawInfo info) throws ModuleException {
        return reloadModule(info, false);
    }

    void unloadModule(String name) throws ModuleException;

    static Modulo create(ModuleClassLoaderFactory<?> classLoaderFactory) {
        return new SimpleModulo(classLoaderFactory);
    }

}
