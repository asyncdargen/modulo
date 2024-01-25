package ru.dargen.modulo.classloader.builtin;

import ru.dargen.modulo.classloader.ModuleClassLoaderFactory;
import ru.dargen.modulo.classloader.url.JarURLStreamHandler;
import ru.dargen.modulo.loader.ModuleRawInfo;

public class BuiltInModuleClassLoaderFactory implements ModuleClassLoaderFactory<BuiltInModuleClassLoader> {

    private final BuiltInComplexModuleClassHolder complexClassHolder;

    public BuiltInModuleClassLoaderFactory(ClassLoader parent) {
        complexClassHolder = new BuiltInComplexModuleClassHolder(parent);
    }

    @Override
    public BuiltInModuleClassLoader createClassLoader(ModuleRawInfo info) {
        return new BuiltInModuleClassLoader(new JarURLStreamHandler(info.entries()), complexClassHolder);
    }

}
