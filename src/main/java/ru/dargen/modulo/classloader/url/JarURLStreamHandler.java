package ru.dargen.modulo.classloader.url;

import lombok.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
public class JarURLStreamHandler extends URLStreamHandler {

    @Getter
    private Map<String, byte[]> entries = new HashMap<>();

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        val entryBytes = entries.get(resolveURLAsEntryName(url));

        if (entryBytes == null) {
            throw new IOException("Entry with name (" + url.getFile() + " not exists");
        }

        return new JarEntryURLConnection(url, entryBytes);
    }

    public void update(Map<String, byte[]> entries) {
        this.entries = entries;
    }

    @SneakyThrows
    public URL createURL() {
        return new URL("dynamic", "", -1, "/", this);
    }

    public void close() {
        entries.clear();
    }

    private static String resolveURLAsEntryName(URL url) {
        val path = url.getFile();
        return path.isEmpty() || path.charAt(0) != '/' ? path : path.substring(1);
    }

}
