package ru.dargen.modulo.util;

public class Timer {

    private static ThreadLocal<Timer> THREAD_LOCAL = ThreadLocal.withInitial(Timer::new);

    private long timestamp;

    public void start() {
        timestamp = System.currentTimeMillis();
    }

    public long end() {
        return System.currentTimeMillis() - timestamp;
    }

    public long restart() {
        try {
            return end();
        } finally {
            start();
        }
    }

    public static Timer get() {
        return THREAD_LOCAL.get();
    }

}
