package ru.dargen.modulo.classloader;

import lombok.Getter;
import ru.dargen.modulo.util.files.ClassFormat;

import java.util.Iterator;

@Getter
public class ModuleClassLoaderIterator implements Iterator<Class<?>> {

    private final ModuleClassLoader classLoader;
    private final Iterator<String> entryIterator;

    private String classEntry;

    public ModuleClassLoaderIterator(ModuleClassLoader classLoader) {
        this.classLoader = classLoader;
        entryIterator = classLoader.getEntries().keySet().iterator();

        iterateNextClassEntry();
    }

    public void iterateNextClassEntry() {
        classEntry = null;
        while (entryIterator.hasNext()) {
            var entry = entryIterator.next();
            if (ClassFormat.isClass(entry)) {
                classEntry = ClassFormat.asClassName(entry);
                break;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return classEntry != null;
    }

    @Override
    public Class<?> next() {
        try {
            return classLoader.findClassOrNull(classEntry);
        } finally {
            iterateNextClassEntry();
        }
    }

}
