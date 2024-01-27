package ru.dargen.modulo.loader;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import ru.dargen.modulo.Modulo;
import ru.dargen.modulo.classloader.ModuleClassLoader;
import ru.dargen.modulo.classloader.ModuleClassLoaderFactory;
import ru.dargen.modulo.loader.exception.ModuleConstructException;
import ru.dargen.modulo.loader.exception.ModuleLoadException;
import ru.dargen.modulo.module.Module;
import ru.dargen.modulo.module.ModuleEntrypoint;
import ru.dargen.modulo.module.ModuleRedefiner;
import ru.dargen.modulo.util.Timer;
import ru.dargen.modulo.util.agent.RedefineHelper;
import ru.dargen.modulo.util.files.IOHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

@UtilityClass
public class ModuleLoader {

    public ModuleRawInfo loadInfo(ModuleProperties properties, Map<String, byte[]> entries) {
        return new ModuleRawInfo(properties, entries);
    }

    public ModuleRawInfo loadInfo(ModuleProperties properties, InputStream input) {
        Map<String, byte[]> entries;
        try {
            entries = IOHelper.readJarEntries(input);
        } catch (IOException e) {
            throw new ModuleLoadException(properties.getName(), "Error while entries loading", e);
        }
        return loadInfo(properties, entries);
    }

    public ModuleRawInfo loadInfo(ModuleProperties properties, byte[] bytes) {
        return loadInfo(properties, IOHelper.createInput(bytes));
    }

    public ModuleRawInfo loadInfo(String rawName, InputStream input) {
        Map<String, byte[]> entries;
        try {
            entries = IOHelper.readJarEntries(input);
        } catch (IOException e) {
            throw new ModuleLoadException(rawName, "Error while entries loading", e);
        }

        ModuleProperties properties;
        try {
            var rawPropertiesBytes = entries.get("module.properties");
            if (rawPropertiesBytes == null) {
                throw new ModuleLoadException(rawName, "Not found module.properties");
            }
            var rawProperties = new Properties();
            rawProperties.load(IOHelper.createInput(rawPropertiesBytes));

            properties = new ModuleProperties(rawProperties);
            properties.validate();
        } catch (IllegalArgumentException | IOException e) {
            throw new ModuleLoadException(rawName, "Error while loading properties", e);
        }

        return loadInfo(properties, entries);
    }

    @SneakyThrows
    public ModuleRawInfo loadInfo(String name, byte[] bytes) {
        return loadInfo(name, IOHelper.createInput(bytes));
    }

    @SneakyThrows
    public ModuleRawInfo loadInfo(Path path) {
        return loadInfo(path.toString(), Files.newInputStream(path));
    }

    @SneakyThrows
    public ModuleRawInfo loadInfo(File file) {
        return loadInfo(file.toPath());
    }

    @SneakyThrows
    public ModuleRawInfo loadInfo(URL url) {
        return loadInfo(url.toString(), url.openStream());
    }

    public Module construct(ModuleRawInfo info, ModuleClassLoaderFactory<?> classLoaderFactory) {
        Timer.get().start();
        ModuleClassLoader classLoader;
        try {
            classLoader = classLoaderFactory.createClassLoader(info);
        } catch (Throwable t) {
            throw new ModuleConstructException(info.properties().getName(), "Error while creating classloader", t);
        }

        Class<? extends ModuleEntrypoint> entrypointClass;
        try {
            entrypointClass = (Class<? extends ModuleEntrypoint>) Class.forName(info.properties().getEntrypoint(), true, (ClassLoader) classLoader);
        } catch (Throwable t) {
            close(classLoader);
            throw new ModuleConstructException(info.properties().getName(), "Error while entrypoint loading", t);
        }

        ModuleEntrypoint entrypoint;
        try {
            if (info.properties().isKotlinObjectEntryPoint()) {
                var entrypointField = entrypointClass.getDeclaredField("INSTANCE");
                entrypointField.trySetAccessible();
                entrypoint = (ModuleEntrypoint) entrypointField.get(null);
            } else entrypoint = entrypointClass.newInstance();
        } catch (Throwable t) {
            close(classLoader);
            throw new ModuleConstructException(info.properties().getName(), "Error while entrypoint creating", t);
        }

        Modulo.LOGGER.info("Module %s constructed took %s ms!".formatted(info.properties().getName(), Timer.get().end()));

        return new Module(info.properties(), entrypoint, classLoader);
    }

    private void close(ModuleClassLoader classLoader) {
        try {
            if (RedefineHelper.isSupported()) {
                ModuleRedefiner.erase(classLoader);
            }
        } catch (Throwable t) {
        }
        try {
            classLoader.close();
        } catch (Throwable t) {
        }
    }

}
