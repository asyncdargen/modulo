package ru.dargen.modulo.classloader.isolated;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.dargen.modulo.classloader.ModuleClassLoaderFactory;
import ru.dargen.modulo.classloader.url.JarURLStreamHandler;
import ru.dargen.modulo.loader.ModuleRawInfo;

@Getter
@RequiredArgsConstructor
public class IsolatedModuleClassLoaderFactory implements ModuleClassLoaderFactory<IsolatedModuleClassLoader> {

    private final ClassLoader parentClassLoader;

    @Override
    public IsolatedModuleClassLoader createClassLoader(ModuleRawInfo info) {
        return new IsolatedModuleClassLoader(new JarURLStreamHandler(info.entries()), parentClassLoader);
    }

}
