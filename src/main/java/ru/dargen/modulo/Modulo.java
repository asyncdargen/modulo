package ru.dargen.modulo;

import ru.dargen.modulo.classloader.ModuleClassLoaderFactory;
import ru.dargen.modulo.classloader.builtin.BuiltInModuleClassLoaderFactory;
import ru.dargen.modulo.loader.ModuleRawInfo;
import ru.dargen.modulo.module.Module;
import ru.dargen.modulo.module.ModuleException;

import java.util.Map;
import java.util.logging.Logger;

public interface Modulo {

    Logger LOGGER = Logger.getLogger("Modulo");

    Map<String, Module> getLoadedModules();

    ModuleClassLoaderFactory<?> getClassLoaderFactory();

    boolean isLoaded(String name);

    Module getModule(String name);

    Module loadModule(ModuleRawInfo info) throws ModuleException;

    boolean reloadModule(ModuleRawInfo info, boolean total) throws ModuleException;

    default boolean reloadModule(ModuleRawInfo info) throws ModuleException {
        return reloadModule(info, false);
    }

    void unloadModule(String name) throws ModuleException;

    static ModuleClassLoaderFactory<?> createClassLoaderFactory(ClassLoader parentClassLoader) {
        return new BuiltInModuleClassLoaderFactory(parentClassLoader);
    }

    static ModuleClassLoaderFactory<?> createClassLoaderFactory() {
        return createClassLoaderFactory(SimpleModulo.class.getClassLoader());
    }

    static SimpleModulo create(ModuleClassLoaderFactory<?> classLoaderFactory) {
        return new SimpleModulo(classLoaderFactory);
    }

    static SimpleModulo create(ClassLoader parentClassLoader) {
        return create(createClassLoaderFactory(parentClassLoader));
    }

    static SimpleModulo create() {
        return create(createClassLoaderFactory());
    }

}
