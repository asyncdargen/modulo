package ru.dargen.modulo.util.agent;


import com.sun.tools.attach.VirtualMachine;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.objectweb.asm.ClassWriter;
import ru.dargen.crowbar.Accessors;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static java.lang.Class.forName;
import static org.objectweb.asm.Opcodes.*;

@UtilityClass
public class AgentHelper {

    private Instrumentation INSTRUMENTATION;

    private final String AGENT_CLASS = "ru.dargen.modulo.generated.Agent";
    private final String AGENT_CLASS_NAME = AGENT_CLASS.replace('.', '/');
    private final String AGENT_INSTRUMENTATION_FIELD = "INSTRUMENTATION";

    private Manifest generateManifest() {
        var manifest = new Manifest();
        var attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(new Attributes.Name("Agent-Class"), AGENT_CLASS);
        attributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
        attributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");
        return manifest;
    }

    @SneakyThrows
    private byte[] generateAgentClass() {
        var node = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.visit(V17, ACC_PUBLIC, AGENT_CLASS.replace('.', '/'), null, "java/lang/Object", null);

        node.visitField(ACC_PUBLIC | ACC_STATIC, "INSTRUMENTATION", "Ljava/lang/instrument/Instrumentation;", null, null);

        var method = node.visitMethod(ACC_PUBLIC | ACC_STATIC, "agentmain", "(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V", null, null);
        method.visitCode();

        //debug
        method.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        method.visitLdcInsn("[Modulo] Agent injected!");
        method.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

        //instrumentation assign
        method.visitVarInsn(ALOAD, 1);
        method.visitFieldInsn(PUTSTATIC, AGENT_CLASS_NAME, AGENT_INSTRUMENTATION_FIELD, "Ljava/lang/instrument/Instrumentation;");
        method.visitInsn(RETURN);
        method.visitEnd();
        method.visitMaxs(1, 2);

        return node.toByteArray();
    }

    @SneakyThrows
    private File createAgentFile() {
        var agentFile = File.createTempFile("modulo-agent", ".jar");
        agentFile.deleteOnExit();

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(agentFile), generateManifest())) {
            var agentJar = new JarEntry(AGENT_CLASS_NAME + ".class");
            jos.putNextEntry(agentJar);
            jos.write(generateAgentClass());
        }

        return agentFile;
    }

    @SneakyThrows
    private Instrumentation injectAgentAndGetInstrumentation() {
        var agentFile = createAgentFile();
        var vm = attachSelfVM();

        vm.loadAgent(agentFile.getAbsolutePath());
        vm.detach();

        return (Instrumentation) forName(AGENT_CLASS).getDeclaredField("INSTRUMENTATION").get(null);
    }

    public Instrumentation instrumentation() {
        return INSTRUMENTATION == null ? (INSTRUMENTATION = injectAgentAndGetInstrumentation()) : INSTRUMENTATION;
    }

    @SneakyThrows
    private VirtualMachine attachSelfVM() {
        allowAttachSelfVM();
        var currentVM = ManagementFactory.getRuntimeMXBean().getName();
        var pid = currentVM.substring(0, currentVM.indexOf('@'));

        return VirtualMachine.attach(pid);
    }

    @SneakyThrows
    private void allowAttachSelfVM() {
        if (Boolean.getBoolean("jdk.attach.allowAttachSelf")) return;

        System.setProperty("jdk.attach.allowAttachSelf", "true");
        var method = Accessors.unsafe().openField(forName("jdk.internal.misc.VM"), "savedProps", Map.class);
        method.setStaticValue(System.getProperties());
    }

}
