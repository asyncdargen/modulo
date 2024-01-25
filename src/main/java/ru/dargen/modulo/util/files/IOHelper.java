package ru.dargen.modulo.util.files;

import lombok.experimental.UtilityClass;
import lombok.val;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

@UtilityClass
public class IOHelper {

    public Map<String, byte[]> readJarEntries(byte[] jarBytes) throws IOException {
        return readJarEntries(new ByteArrayInputStream(jarBytes));
    }

    public Map<String, byte[]> readJarEntries(InputStream inputStream) throws IOException {
        val entries = new HashMap<String, byte[]>();
        try (val jarInputStream = new JarInputStream(inputStream)) {
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null)
                if (!entry.isDirectory()) entries.put(entry.getName(), readAllBytes(jarInputStream, false));
        } finally {
            inputStream.close();
        }
        return entries;
    }

    public byte[] readAllBytes(InputStream inputStream) throws IOException {
        return readAllBytes(inputStream, true);
    }

    public byte[] readAllBytes(InputStream inputStream, boolean closeIn) throws IOException {
        var buffer = new byte[1024 * 4];
        var length = 0;
        try (val outputStream = new ByteArrayOutputStream()) {
            while ((length = inputStream.read(buffer)) != -1)
                outputStream.write(buffer, 0, length);
            buffer = outputStream.toByteArray();
        } finally {
            if (closeIn) inputStream.close();
        }

        return buffer;
    }

    public InputStream createInput(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

}
