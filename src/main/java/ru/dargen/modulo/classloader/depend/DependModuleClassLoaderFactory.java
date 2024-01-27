package ru.dargen.modulo.classloader.depend;

import ru.dargen.modulo.classloader.ModuleClassLoaderFactory;
import ru.dargen.modulo.classloader.url.JarURLStreamHandler;
import ru.dargen.modulo.loader.ModuleRawInfo;

public class DependModuleClassLoaderFactory implements ModuleClassLoaderFactory<DependModuleClassLoader> {

    private final DependModuleClassHolder complexClassHolder;

    public DependModuleClassLoaderFactory(ClassLoader parent) {
        complexClassHolder = new DependModuleClassHolder(parent);
    }

    @Override
    public ClassLoader getParentClassLoader() {
        return complexClassHolder.getParentClassLoader();
    }

    @Override
    public DependModuleClassLoader createClassLoader(ModuleRawInfo info) {
        return new DependModuleClassLoader(new JarURLStreamHandler(info.entries()), complexClassHolder);
    }

}
