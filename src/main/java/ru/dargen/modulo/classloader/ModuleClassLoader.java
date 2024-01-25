package ru.dargen.modulo.classloader;

import ru.dargen.modulo.module.Module;

import java.util.Iterator;
import java.util.Map;

public interface ModuleClassLoader extends Iterable<Class<?>> {

    Module getModule();

    void setModule(Module module);

    Class<?> findClassOrNull(String name);

    Class<?> getLoadedClass(String name);

    Class<?> invalidateLoadedClass(String name);

    boolean isLoadedClass(String name);

    Map<String, byte[]> getEntries();

    void updateEntries(Map<String, byte[]> entries);

    void close();

    default Iterator<Class<?>> iterator() {
        return new ModuleClassLoaderIterator(this);
    }

}
