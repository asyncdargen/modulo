package ru.dargen.modulo.classloader.depend;

import lombok.Getter;
import lombok.SneakyThrows;
import ru.dargen.modulo.classloader.url.JarURLStreamHandler;
import ru.dargen.modulo.classloader.url.ModuleURLClassLoader;

@Getter
public class DependModuleClassLoader extends ModuleURLClassLoader {

    private final DependModuleProperties properties;
    private final DependModuleClassHolder complexClassLoader;

    @SneakyThrows
    public DependModuleClassLoader(JarURLStreamHandler jarURLStreamHandler, DependModuleClassHolder complexClassLoader) {
        super(jarURLStreamHandler, null);

        this.properties = new DependModuleProperties(module.getName(), module.getProperties().getRawProperties());
        this.complexClassLoader = complexClassLoader;
        complexClassLoader.addClassLoader(this);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        var clazz = findClassOrNull(name);

        if (clazz == null) {
            clazz = complexClassLoader.findClass(name, this);
        }

        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }

        return clazz;
    }

    @Override
    public Class<?> findClassOrNull(String name) {
        var clazz = super.findClassOrNull(name);

        if (clazz != null) {
            complexClassLoader.getLoadedClasses().put(name, clazz);
        }

        return clazz;
    }

    @Override
    public Class<?> invalidateLoadedClass(String name) {
        complexClassLoader.getLoadedClasses().remove(name);
        return super.invalidateLoadedClass(name);
    }

    @Override
    @SneakyThrows
    public void close() {
        complexClassLoader.removeClassLoader(this);
        super.close();
    }

}
