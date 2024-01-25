package ru.dargen.modulo.classloader.url;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class JarEntryURLConnection extends URLConnection {

    private final byte[] entryBytes;

    protected JarEntryURLConnection(URL url, byte[] entryBytes) {
        super(url);
        this.entryBytes = entryBytes;
    }

    @Override
    public void connect() throws IOException {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(entryBytes);
    }

}
