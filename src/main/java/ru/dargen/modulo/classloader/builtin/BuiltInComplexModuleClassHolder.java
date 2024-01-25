package ru.dargen.modulo.classloader.builtin;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public class BuiltInComplexModuleClassHolder {

    private final ClassLoader parentClassLoader;

    private final Set<BuiltInModuleClassLoader> classLoaders = Collections.newSetFromMap(new ConcurrentHashMap<>());
    @Getter(AccessLevel.PACKAGE)
    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

    public Class<?> findClass(String name) {
        return findClass(name, null);
    }

    public Class<?> findClass(String name, BuiltInModuleClassLoader callerClassLoader) {
        var clazz = loadedClasses.get(name);

        if (clazz == null) {
            try {
                clazz = parentClassLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
            }
        }

        if (clazz == null) {
            for (BuiltInModuleClassLoader classLoader : classLoaders) {
                if (classLoader == callerClassLoader) continue;

                if ((clazz = classLoader.findClassOrNull(name)) != null) {
                    loadedClasses.put(name, clazz);
                    break;
                }
            }
        }

        if (clazz != null
                && clazz.getClassLoader() instanceof BuiltInModuleClassLoader loader
                && callerClassLoader.getModule() != null && !callerClassLoader.getModule().hasAccessTo(loader.getModule())
        ) clazz = null;

        return clazz;
    }

    public void addClassLoader(BuiltInModuleClassLoader classLoader) {
        classLoaders.add(classLoader);
    }

    public void removeClassLoader(BuiltInModuleClassLoader classLoader) {
        classLoaders.remove(classLoader);
        loadedClasses.values().removeIf(clazz -> clazz.getClassLoader() == classLoader);
    }

}
