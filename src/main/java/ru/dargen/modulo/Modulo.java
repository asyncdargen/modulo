package ru.dargen.modulo;

import lombok.Getter;
import ru.dargen.modulo.classloader.ModuleClassLoaderFactory;
import ru.dargen.modulo.classloader.builtin.BuiltInModuleClassLoaderFactory;
import ru.dargen.modulo.loader.ModuleLoader;
import ru.dargen.modulo.loader.ModuleRawInfo;
import ru.dargen.modulo.module.Module;
import ru.dargen.modulo.module.ModuleException;
import ru.dargen.modulo.module.ModuleRedefiner;
import ru.dargen.modulo.util.Timer;
import ru.dargen.modulo.util.agent.RedefineHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@Getter
public class Modulo {

    public static final Logger LOGGER = Logger.getLogger("Modulo");

    private final Lock lock = new ReentrantLock();
    private final Map<String, Module> loadedModules = new ConcurrentHashMap<>();

    private final ModuleClassLoaderFactory<?> classLoaderFactory;

    static {
        for (Handler handler : LOGGER.getParent().getHandlers()) {
            handler.setFormatter(new SimpleFormatter());
        }
        RedefineHelper.init();
    }

    public Modulo(ModuleClassLoaderFactory<?> classLoaderFactory) {
        this.classLoaderFactory = classLoaderFactory;
    }

    public boolean isLoaded(String name) {
        return loadedModules.containsKey(name);
    }

    public Module getModule(String name) {
        try {
            lock.lock();
            return getModule0(name);
        } finally {
            lock.unlock();
        }
    }

    private Module getModule0(String name) {
        return loadedModules.get(name);
    }

    public Module loadModule(ModuleRawInfo info) {
        if (isLoaded(info.getName())) throw new ModuleException(info.getName(), "Already loaded");

        try {
            lock.lock();

            Module module;
            try {
                module = ModuleLoader.construct(info, classLoaderFactory);
            } catch (Throwable t) {
                throw new ModuleException(info.getName(), "Error while construct", t);
            }

            loadModule0(module);

            return module;
        } finally {
            lock.unlock();
        }
    }

    private void loadModule0(Module module) {
        Timer.get().start();

        try {
            module.enable();
        } catch (ModuleException ex) {
            unloadModuleSafe0(module);
            throw ex;
        }

        if (module.isEnabled()) {
            Modulo.LOGGER.info("Module %s enabled took %s ms!".formatted(module.getName(), Timer.get().end()));
            loadedModules.put(module.getName(), module);
        }
    }

    public boolean reloadModule(ModuleRawInfo info) {
        var moduleName = info.getName();

        if (!RedefineHelper.isSupported() && ModuleRedefiner.RELOAD_IF_REDEFINE_NOT_SUPPORTED) {
            try {
                unloadModule(moduleName);
            } catch (Throwable ignored) {
            }
            return loadModule(info).isEnabled();
        } else if (isLoaded(moduleName)) {
            return reloadModule0(getModule0(info.getName()), info);
        }

        return false;
    }

    private boolean reloadModule0(Module module, ModuleRawInfo info) {
        try {
            lock.lock();

            Timer.get().start();
            try {
                var metrics = ModuleRedefiner.reload(module, info.entries());
                module.setProperties(info.properties());
                Modulo.LOGGER.info("Module entries %s reloaded took %s ms! %s".formatted(module.getName(), Timer.get().restart(), metrics.toString()));

                module.reload();
                Modulo.LOGGER.info("Module %s reloaded took %s ms!".formatted(module.getName(), Timer.get().end()));
            } catch (Throwable t) {
                throw new ModuleException(module.getName(), "Error while reload", t);
            }
        } finally {
            lock.unlock();
        }

        return true;
    }

    public void unloadModule(String name) {
        if (!isLoaded(name)) throw new ModuleException(name, "Not loaded");

        try {
            lock.lock();

            if (isLoaded(name)) {
                var module = getModule0(name);
                unloadModule0(module);
            }
        } finally {
            lock.unlock();
        }
    }

    private void unloadModuleSafe0(Module module) {
        try {
            unloadModule0(module);
        } catch (Throwable ignored) {

        }
    }

    private void unloadModule0(Module module) {
        Timer.get().start();

        ModuleException exception = null;

        try {
            module.disable();
        } catch (ModuleException ex) {
            exception = ex;
        }

        try {
            if (RedefineHelper.isSupported()) {
                ModuleRedefiner.erase(module.getClassLoader());
            }
        } catch (Throwable t) {
            var ex = new ModuleException(module.getName(), "Error while classes erasing", t);
            if (exception == null) exception = ex;
            else exception.addSuppressed(ex);
        }

        try {
            module.getClassLoader().close();
        } catch (Throwable t) {
            var ex = new ModuleException(module.getName(), "Error while closing", t);
            if (exception == null) exception = ex;
            else exception.addSuppressed(ex);
        }

        Modulo.LOGGER.info("Module %s disabled took %s ms!".formatted(module.getName(), Timer.get().end()));
        loadedModules.remove(module.getName());

        if (exception != null) {
            throw exception;
        }
    }

    public static ModuleClassLoaderFactory<?> createClassLoaderFactory(ClassLoader parentClassLoader) {
        return new BuiltInModuleClassLoaderFactory(parentClassLoader);
    }

    public static ModuleClassLoaderFactory<?> createClassLoaderFactory() {
        return createClassLoaderFactory(Modulo.class.getClassLoader());
    }

    public static Modulo create(ModuleClassLoaderFactory<?> classLoaderFactory) {
        return new Modulo(classLoaderFactory);
    }

    public static Modulo create(ClassLoader parentClassLoader) {
        return create(createClassLoaderFactory(parentClassLoader));
    }

    public static Modulo create() {
        return create(createClassLoaderFactory());
    }

}
