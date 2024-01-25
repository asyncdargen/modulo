package ru.dargen.modulo.classloader.builtin;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import ru.dargen.modulo.classloader.ModuleClassLoader;
import ru.dargen.modulo.classloader.url.JarURLStreamHandler;
import ru.dargen.modulo.module.Module;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class BuiltInModuleClassLoader extends URLClassLoader implements ModuleClassLoader {

    @Setter
    private Module module;
    private final BuiltInComplexModuleClassHolder complexClassLoader;
    private final JarURLStreamHandler jarURLStreamHandler;
    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

    @SneakyThrows
    public BuiltInModuleClassLoader(JarURLStreamHandler jarURLStreamHandler, BuiltInComplexModuleClassHolder complexClassLoader) {
        super(new URL[]{jarURLStreamHandler.createURL()}, null);
        this.complexClassLoader = complexClassLoader;
        this.jarURLStreamHandler = jarURLStreamHandler;
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
        var clazz = findLoadedClass(name);

        if (clazz == null) try {
            clazz = findClass(name);
            loadedClasses.put(name, clazz);
            complexClassLoader.getLoadedClasses().put(name, clazz);
        } catch (Throwable t) {
        }

        return clazz;
    }

    @Override
    public Class<?> getLoadedClass(String name) {
        return loadedClasses.get(name);
    }

    @Override
    public Class<?> invalidateLoadedClass(String name) {
        complexClassLoader.getLoadedClasses().remove(name);
        return loadedClasses.remove(name);
    }

    @Override
    public boolean isLoadedClass(String name) {
        return loadedClasses.containsKey(name);
    }

    @Override
    public Map<String, byte[]> getEntries() {
        return jarURLStreamHandler.getEntries();
    }

    @Override
    public void updateEntries(Map<String, byte[]> entries) {
        jarURLStreamHandler.update(entries);
    }

    @Override
    @SneakyThrows
    public void close() {
        complexClassLoader.removeClassLoader(this);
        loadedClasses.clear();
        jarURLStreamHandler.close();
        super.close();
    }

}
