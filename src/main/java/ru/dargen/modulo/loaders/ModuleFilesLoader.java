package ru.dargen.modulo.loaders;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.dargen.modulo.Modulo;
import ru.dargen.modulo.loader.ModuleLoader;
import ru.dargen.modulo.loader.ModuleRawInfo;
import ru.dargen.modulo.module.Module;

import java.io.File;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static java.nio.file.StandardWatchEventKinds.*;

public class ModuleFilesLoader extends Thread {

    private final Modulo modulo;
    private final Map<String, Module> loadedModules = new ConcurrentHashMap<>();

    private final Path folder;
    private final WatchService service;

    @SneakyThrows
    public ModuleFilesLoader(Modulo modulo, Path folder) {
        super("ModuleFilesLoader-Thread");
        setDaemon(true);

        this.modulo = modulo;

        this.folder = folder;
        service = FileSystems.getDefault().newWatchService();

        start();
    }

    public static ModuleFilesLoader watch(Modulo modulo, Path folder) {
        return new ModuleFilesLoader(modulo, folder);
    }

    public static ModuleFilesLoader watch(Modulo modulo, File folder) {
        return watch(modulo, folder.toPath());
    }

    public static ModuleFilesLoader watch(Modulo modulo, String folder) {
        return watch(modulo, Paths.get(folder));
    }

    @SneakyThrows
    private void loadInitialModules() {
        var modulesPaths = Files.list(folder).filter(file -> file.toString().endsWith(".jar")).toList();

        for (Path path : modulesPaths) {
            var module = updateModule(path, UpdateAction.LOAD);
            if (module != null && module.isEnabled()) {
                loadedModules.put(path.toString(), module);
            }
        }
    }

    @SneakyThrows
    @Override
    public void run() {
        folder.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

        loadInitialModules();

        while (!isInterrupted()) {
            WatchKey key = null;
            try {
                key = service.take();
                key.pollEvents().forEach(event -> {
                    var action = UpdateAction.byKind(event.kind());
                    if (action != null && event.context() instanceof Path filePath) {
                        var path = folder.resolve(filePath);
                        var module = updateModule(path, action);

                        if (module != null && module.isEnabled()) {
                            loadedModules.put(path.toString(), module);
                        }
                    }
                });
            } catch (Throwable throwable) {
                Modulo.LOGGER.log(Level.SEVERE, "Error while file keys poll", throwable);
            } finally {
                if (key != null) key.reset();
            }
        }
    }

    public Module updateModule(Path path, UpdateAction action) {
        ModuleRawInfo info = null;
        try {
            info = ModuleLoader.load(path);
        } catch (Throwable t) {
            if (action != UpdateAction.UNLOAD) {
                Modulo.LOGGER.log(Level.SEVERE, "Load error ", t);
                return null;
            }
        }

        return updateModule(info, path, action);
    }

    public Module updateModule(ModuleRawInfo info, Path path, UpdateAction action) {
        var loadedModule = loadedModules.get(path.toString());

        return switch (action) {
            case UNLOAD -> {
                if (loadedModule != null) {
                    action.runAction(modulo, loadedModule.constructRawInfo());
                    loadedModules.remove(path.toString());
                }

                yield loadedModule;
            }
            case LOAD -> {
                if (loadedModule != null) {
                    updateModule(loadedModule.constructRawInfo(), path, UpdateAction.UNLOAD);
                }

                yield action.runAction(modulo, info);
            }
            case RELOAD -> {
                if (loadedModule != null) {
                    if (!loadedModule.getProperties().equalsBase(info.properties())) {
                        updateModule(loadedModule.constructRawInfo(), path, UpdateAction.UNLOAD);
                        yield updateModule(info, path, UpdateAction.LOAD);
                    }

                    yield action.runAction(modulo, info);
                }

                yield updateModule(info, path, UpdateAction.LOAD);
            }
        };
    }

    @Getter
    @RequiredArgsConstructor
    public enum UpdateAction {

        LOAD(ENTRY_CREATE) {
            @Override
            protected Module runAction(Modulo modulo, ModuleRawInfo info) {
                return modulo.loadModule(info);
            }
        }, RELOAD(ENTRY_MODIFY) {
            @Override
            protected Module runAction(Modulo modulo, ModuleRawInfo info) {
                modulo.reloadModule(info);
                return modulo.getModule(info.getName());
            }
        }, UNLOAD(ENTRY_DELETE) {
            @Override
            protected Module runAction(Modulo modulo, ModuleRawInfo info) {
                modulo.unloadModule(info.getName());
                return null;
            }
        };

        private final WatchEvent.Kind<?> kind;

        public Module run(Modulo modulo, ModuleRawInfo info) {
            try {
                return runAction(modulo, info);
            } catch (Throwable throwable) {
                Modulo.LOGGER.log(Level.SEVERE, "Error while processing action %s".formatted(name()), throwable);
            }

            return null;
        }

        protected abstract Module runAction(Modulo modulo, ModuleRawInfo info);

        public static UpdateAction byKind(WatchEvent.Kind<?> kind) {
            for (UpdateAction value : values()) {
                if (value.kind == kind) {
                    return value;
                }
            }

            return null;
        }

    }

}
