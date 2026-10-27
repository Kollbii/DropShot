package pl.maciek.squashscoreboard;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * [PERF-120HZ] [ARCH-UDF]
 * Dedicated sequential background I/O executor and main-thread dispatcher
 * to ensure all JSON serialization and AES-256-GCM encryption execute off the UI thread.
 */
public final class AsyncRepositoryExecutor {
    private static final ExecutorService IO_POOL = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "dropshot-repo-io");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });

    private AsyncRepositoryExecutor() {}

    public static void runIo(Runnable task) {
        if (task != null) {
            IO_POOL.execute(task);
        }
    }

    public static void runOnMain(Runnable task) {
        if (task == null) return;
        try {
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper != null) {
                if (Looper.myLooper() == mainLooper) {
                    task.run();
                } else {
                    new Handler(mainLooper).post(task);
                }
                return;
            }
        } catch (Throwable ignored) {
            // Fallback for pure JVM unit test environment
        }
        task.run();
    }
}

