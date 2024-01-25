package ru.dargen.modulo.classloader;

import ru.dargen.modulo.loader.ModuleRawInfo;

public interface ModuleClassLoaderFactory<M extends ClassLoader & ModuleClassLoader> {

    M createClassLoader(ModuleRawInfo info);

}
