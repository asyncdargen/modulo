package ru.dargen.modulo.util.agent;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import ru.dargen.modulo.Modulo;

import java.lang.instrument.ClassDefinition;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;

@UtilityClass
public class RedefineHelper {

    private final boolean REDEFINE_SUPPORTED =
            Boolean.parseBoolean(System.getProperty("modulo.redefine", "true"))
                    && getRuntimeMXBean().getInputArguments().contains("-XX:+AllowEnhancedClassRedefinition");

    public void requireSupport() {
        if (!isSupported()) throw new UnsupportedOperationException("Class redefinition not supported");
    }

    public boolean isSupported() {
        return REDEFINE_SUPPORTED;
    }

    public void init() {
        if (isSupported()) {
            AgentHelper.instrumentation();
            Modulo.LOGGER.info("Instrumentation created!");
        } else Modulo.LOGGER.warning("Class redefinition not supported!");
    }

    @SneakyThrows
    public void redefine(Class<?> clazz, byte[] bytes) {
        AgentHelper.instrumentation().redefineClasses(new ClassDefinition(clazz, bytes));
    }

    @SneakyThrows
    public void redefine(Map<Class<?>, byte[]> redefineMap) {
        redefineMap.forEach(RedefineHelper::redefine);
    }

    @SneakyThrows
    public void erase(Class<?> clazz) {
        redefine(clazz, generateEmptyClass(clazz));
    }

    @SneakyThrows
    public void erase(Collection<Class<?>> classes) {
        redefine(classes.stream().collect(Collectors.toMap(clazz -> clazz, RedefineHelper::generateEmptyClass)));
    }

    public byte[] generateEmptyClass(Class<?> clazz) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V17, 0,
                clazz.getName().replace('.', '/'),
                null,
                Type.getInternalName(clazz.getSuperclass()),
                Arrays.stream(clazz.getInterfaces()).map(Type::getInternalName).toArray(String[]::new));
        return cw.toByteArray();
    }

}
