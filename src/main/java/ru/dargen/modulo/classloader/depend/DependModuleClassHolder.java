package ru.dargen.modulo.classloader.depend;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public class DependModuleClassHolder {

    private final ClassLoader parentClassLoader;

    private final Set<DependModuleClassLoader> classLoaders = Collections.newSetFromMap(new ConcurrentHashMap<>());
    @Getter(AccessLevel.PACKAGE)
    private final Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

    public Class<?> findClass(String name) {
        return findClass(name, null);
    }

    public Class<?> findClass(String name, DependModuleClassLoader callerClassLoader) {
        var clazz = loadedClasses.get(name);

        if (clazz == null) {
            try {
                clazz = parentClassLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
            }
        }

        if (clazz == null) {
            for (DependModuleClassLoader classLoader : classLoaders) {
                if (classLoader == callerClassLoader) continue;

                if ((clazz = classLoader.findClassOrNull(name)) != null) {
                    loadedClasses.put(name, clazz);
                    break;
                }
            }
        }

        if (clazz != null
                && clazz.getClassLoader() instanceof DependModuleClassLoader loader
                && callerClassLoader.getModule() != null && !callerClassLoader.getProperties().hasAccessTo(loader.getProperties())
        ) clazz = null;

        return clazz;
    }

    public void addClassLoader(DependModuleClassLoader classLoader) {
        classLoaders.add(classLoader);
    }

    public void removeClassLoader(DependModuleClassLoader classLoader) {
        classLoaders.remove(classLoader);
        loadedClasses.values().removeIf(clazz -> clazz.getClassLoader() == classLoader);
    }

}
