package ru.dargen.modulo.util;

public class Timer {

    private static ThreadLocal<Timer> THREAD_LOCAL = new ThreadLocal<>() {
        @Override
        protected Timer initialValue() {
            return new Timer();
        }
    };

    private long timestamp;

    public void start() {
        timestamp = System.currentTimeMillis();
    }

    public long end() {
        return System.currentTimeMillis() - timestamp;
    }

    public long restart() {
        var value = end();
        start();
        return value;
    }

    public static Timer get() {
        return THREAD_LOCAL.get();
    }

}
