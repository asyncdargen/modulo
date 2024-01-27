package ru.dargen.modulo.classloader.url;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import ru.dargen.modulo.classloader.ModuleClassLoader;
import ru.dargen.modulo.module.Module;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ModuleURLClassLoader extends URLClassLoader implements ModuleClassLoader {

    @Setter
    protected Module module;

    protected final JarURLStreamHandler jarURLStreamHandler;
    protected final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

    public ModuleURLClassLoader(JarURLStreamHandler jarURLStreamHandler, ClassLoader parent) {
        super(new URL[]{jarURLStreamHandler.createURL()}, parent);
        this.jarURLStreamHandler = jarURLStreamHandler;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        var clazz = findClassOrNull(name);

        if (clazz == null) {
            if (getParent() != null) clazz = getParent().loadClass(name);
            else throw new ClassNotFoundException(name);
        }

        return clazz;
    }

    @Override
    public Class<?> findClassOrNull(String name) {
        var clazz = findLoadedClass(name);

        if (clazz == null) try {
            clazz = findClass(name);
            if (clazz != null) loadedClasses.put(name, clazz);
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
        loadedClasses.clear();
        jarURLStreamHandler.close();
        super.close();
    }

}
