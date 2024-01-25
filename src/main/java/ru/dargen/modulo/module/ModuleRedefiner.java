package ru.dargen.modulo.module;

import lombok.experimental.UtilityClass;
import ru.dargen.modulo.classloader.ModuleClassLoader;
import ru.dargen.modulo.util.agent.RedefineHelper;
import ru.dargen.modulo.util.files.ClassFormat;

import java.util.Map;

import static java.lang.Boolean.parseBoolean;

@UtilityClass
public class ModuleRedefiner {

    public boolean COMPUTE_REDEFINE_CLASSES_HASHES = parseBoolean(System.getProperty("modulo.redefine-compute-hash", "true"));
//    public boolean ERASE_DELETED_CLASSES = parseBoolean(System.getProperty("modulo.erase-deleted", "true"));
    public boolean ERASE_UNLOADED_CLASSES = parseBoolean(System.getProperty("modulo.erase-unloaded", "true"));
    public boolean RELOAD_IF_REDEFINE_NOT_SUPPORTED = parseBoolean(System.getProperty("modulo.force-reload", "true"));

    public void erase(ModuleClassLoader classLoader) {
        RedefineHelper.requireSupport();

        if (ERASE_UNLOADED_CLASSES) {
            classLoader.getEntries().keySet()
                    .stream()
                    .filter(ClassFormat::isClass)
                    .map(ClassFormat::asClassName)
                    .filter(classLoader::isLoadedClass)
                    .map(classLoader::getLoadedClass)
                    .forEach(RedefineHelper::erase);
        }
    }

    public Metrics reload(Module module, Map<String, byte[]> entries) {
        RedefineHelper.requireSupport();

        var classLoader = module.getClassLoader();
        var oldEntries = classLoader.getEntries();

        var deletedCount = 0;
        var updatedCount = 0;
        var redefinedClassesCount = 0;
        var addedCount = 0;

        for (String entry : entries.keySet()) {
            if (!oldEntries.containsKey(entry)) addedCount++;
            else {
                updatedCount++;
                if (ClassFormat.isClass(entry)) {
                    var className = ClassFormat.asClassName(entry);
                    if (classLoader.isLoadedClass(className)
                            && (!COMPUTE_REDEFINE_CLASSES_HASHES
                            || !ClassFormat.computeHash(oldEntries.get(entry), entries.get(entry)))) {
                        redefinedClassesCount++;
                        RedefineHelper.redefine(classLoader.getLoadedClass(className), entries.get(entry));
                    }
                }
            }
        }
        for (String entry : oldEntries.keySet()) {
            if (!entries.containsKey(entry)) {
                deletedCount++;
//                if (ClassFormat.isClass(entry)) {
//                    var clazz = classLoader.invalidateLoadedClass(ClassFormat.asClassName(entry));
//                    if (clazz != null && ERASE_DELETED_CLASSES) RedefineHelper.erase(clazz);
//                }
            }
        }

        classLoader.updateEntries(entries);

        return new Metrics(oldEntries.size(), entries.size(), updatedCount, redefinedClassesCount, addedCount, deletedCount);
    }

    public record Metrics(int oldEntriesSize, int newEntriesSize,
                          int updatedEntries, int redefinedClasses,
                          int addedEntries, int deletedEntries) {

        @Override
        public String toString() {
            return "(entries: %s -> %s | updates: %s, +%s, -%s | redefines: %s)".formatted(
                    oldEntriesSize, newEntriesSize, updatedEntries, addedEntries, deletedEntries, redefinedClasses
            );
        }

    }


}
