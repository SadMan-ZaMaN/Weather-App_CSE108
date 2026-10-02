package com.weatherfx.ui;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Swaps screens inside one window. The main screen is loaded once and kept, so coming back
 * from the forecast or comments doesn't wipe out the weather you were looking at.
 */
public class Navigator {

    private final StackPane container;
    private AppContext context;
    private Parent mainView;

    public Navigator(StackPane container) {
        this.container = container;
    }

    public void attach(AppContext context) {
        this.context = context;
    }

    public void showMain() {
        if (mainView == null) {
            mainView = load("main-view.fxml").getRoot();
        }
        swapTo(mainView);
    }

    /** Loads a screen, shows it, and returns its controller so the caller can pass data in. */
    public <T> T show(String fxml) {
        FXMLLoader loader = load(fxml);
        swapTo(loader.getRoot());
        return loader.getController();
    }

    private FXMLLoader load(String fxml) {
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/com/weatherfx/views/" + fxml));
        loader.setControllerFactory(type -> {
            try {
                return type.getConstructor(AppContext.class).newInstance(context);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Can't create " + type.getSimpleName(), e);
            }
        });
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't load " + fxml, e);
        }
        return loader;
    }

    private void swapTo(Parent view) {
        view.setOpacity(0);
        container.getChildren().setAll(view);
        FadeTransition fade = new FadeTransition(Duration.millis(220), view);
        fade.setToValue(1);
        fade.play();
    }
}
