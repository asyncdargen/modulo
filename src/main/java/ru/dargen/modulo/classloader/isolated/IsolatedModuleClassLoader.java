package ru.dargen.modulo.classloader.isolated;

import lombok.Getter;
import ru.dargen.modulo.classloader.url.JarURLStreamHandler;
import ru.dargen.modulo.classloader.url.ModuleURLClassLoader;

@Getter
public class IsolatedModuleClassLoader extends ModuleURLClassLoader {

    public IsolatedModuleClassLoader(JarURLStreamHandler jarURLStreamHandler, ClassLoader parent) {
        super(jarURLStreamHandler, parent);
    }

}
