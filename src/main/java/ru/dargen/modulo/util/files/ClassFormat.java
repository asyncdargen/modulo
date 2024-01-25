package ru.dargen.modulo.util.files;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ClassFormat {

    public boolean computeHash(byte[] first, byte[] second) {
        return first.length == second.length
                && Hashes.sha256(first).equals(Hashes.sha256(second));
    }

    public boolean isClass(String name) {
        return name.endsWith(".class");
    }

    public String asClassName(String name) {
        return name.substring(0, name.lastIndexOf('.')).replace('/', '.');
    }

}
