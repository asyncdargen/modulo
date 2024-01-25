package ru.dargen.modulo.util.files;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.security.MessageDigest;

@UtilityClass
public class Hashes {

    private final ThreadLocal<MessageDigest> DIGESTS = new ThreadLocal<>() {
        @Override
        @SneakyThrows
        protected MessageDigest initialValue() {
            return MessageDigest.getInstance("SHA256");
        }
    };

    private String hex(byte[] bytes) {
        var builder = new StringBuilder();
        for (byte part : bytes) {
            builder.append("%x".formatted(part));
        }
        return builder.toString();
    }

    public String sha256(byte[] bytes) {
        try {
            return hex(DIGESTS.get().digest(bytes));
        } finally {
            DIGESTS.get().reset();
        }
    }

}
