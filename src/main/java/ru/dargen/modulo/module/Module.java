package ru.dargen.modulo.module;

import lombok.Getter;
import lombok.Setter;
import ru.dargen.modulo.classloader.ModuleClassLoader;
import ru.dargen.modulo.loader.ModuleProperties;
import ru.dargen.modulo.loader.ModuleRawInfo;

@Getter
@Setter
public class Module {

    private boolean enabled;

    private ModuleProperties properties;
    private final ModuleEntrypoint entrypoint;
    private final ModuleClassLoader classLoader;

    public Module(ModuleProperties properties, ModuleEntrypoint entrypoint, ModuleClassLoader classLoader) {
        this.properties = properties;
        this.entrypoint = entrypoint;
        this.classLoader = classLoader;

        classLoader.setModule(this);
    }

    public String getName() {
        return properties.getName();
    }

    public void enable() throws ModuleException {
        if (!enabled) try {
            entrypoint.enable();
            enabled = true;
        } catch (Throwable t) {
            enabled = false;
            throw new ModuleException(getName(), "Error while enabling", t);
        }
    }

    public void reload() throws ModuleException {
        if (enabled) try {
            entrypoint.reload();
        } catch (Throwable t) {
            throw new ModuleException(getName(), "Error while reloading", t);
        }
    }

    public void disable() throws ModuleException {
        if (enabled) try {
            enabled = false;
            entrypoint.disable();
        } catch (Throwable t) {
            throw new ModuleException(getName(), "Error while disabling", t);
        }
    }

    public ModuleRawInfo constructRawInfo() {
        return new ModuleRawInfo(properties, classLoader.getEntries());
    }

    public boolean hasAccessTo(Module module) {
        return (!module.getProperties().isIsolated() && !getProperties().isIsolated())
                || (module.getProperties().isForceDepend() || getProperties().isForceDepend())
                || getProperties().getDepends().contains(module.getName());
    }

}
