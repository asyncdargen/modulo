package ru.dargen.modulo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.dargen.modulo.classloader.ModuleClassLoaderFactory;
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

@Getter
@RequiredArgsConstructor
public class SimpleModulo implements Modulo {

    private final Lock lock = new ReentrantLock();
    private final Map<String, Module> loadedModules = new ConcurrentHashMap<>();

    private final ModuleClassLoaderFactory<?> classLoaderFactory;

    static {
        RedefineHelper.init();
    }

    @Override
    public boolean isLoaded(String name) {
        return loadedModules.containsKey(name);
    }

    @Override
    public Module getModule(String name) {
        try {
            lock.lock();
            return getModule0(name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Module loadModule(ModuleRawInfo info) throws ModuleException {
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

    @Override
    public boolean reloadModule(ModuleRawInfo info, boolean total) throws ModuleException {
        var moduleName = info.getName();

        if (total || !RedefineHelper.isSupported() && ModuleRedefiner.RELOAD_IF_REDEFINE_NOT_SUPPORTED) {
            try {
                unloadModule(moduleName);
            } catch (Throwable ignored) {
            }
            return loadModule(info).isEnabled();
        } else if (RedefineHelper.isSupported() && isLoaded(moduleName)) {
            return reloadModule0(getModule0(info.getName()), info);
        }

        return false;
    }

    @Override
    public void unloadModule(String name) throws ModuleException {
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

    private Module getModule0(String name) {
        return loadedModules.get(name);
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

}
