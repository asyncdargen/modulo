package ru.dargen.modulo.classloader;

import ru.dargen.modulo.Modulo;
import ru.dargen.modulo.classloader.depend.DependModuleClassLoaderFactory;
import ru.dargen.modulo.classloader.isolated.IsolatedModuleClassLoaderFactory;
import ru.dargen.modulo.loader.ModuleRawInfo;

public interface ModuleClassLoaderFactory<M extends ClassLoader & ModuleClassLoader> {

    ClassLoader getParentClassLoader();

    M createClassLoader(ModuleRawInfo info);

    static ModuleClassLoaderFactory<?> depend(ClassLoader parentClassLoader) {
        return new DependModuleClassLoaderFactory(parentClassLoader);
    }

    static ModuleClassLoaderFactory<?> depend() {
        return depend(Modulo.class.getClassLoader());
    }

    static ModuleClassLoaderFactory<?> isolated(ClassLoader parentClassLoader) {
        return new IsolatedModuleClassLoaderFactory(parentClassLoader);
    }

    static ModuleClassLoaderFactory<?> isolated() {
        return isolated(Modulo.class.getClassLoader());
    }

}
