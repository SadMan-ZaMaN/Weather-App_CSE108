package com.weatherfx.ui;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class Async {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3, task -> {
        Thread thread = new Thread(task, "weatherfx-worker");
        thread.setDaemon(true);
        return thread;
    });

    private Async() {
    }

    static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        POOL.execute(() -> {
            try {
                T result = work.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                Platform.runLater(() -> onFailure.accept(e));
            }
        });
    }
}
