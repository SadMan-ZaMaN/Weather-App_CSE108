package com.weatherfx.ui;

import com.weatherfx.comments.Comment;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CommentsController {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("MMM dd, HH:mm", Locale.ENGLISH);

    @FXML private Label serverLabel;
    @FXML private VBox commentsContainer;
    @FXML private Label messageLabel;
    @FXML private TextField usernameField;
    @FXML private TextArea commentInput;
    @FXML private Label counterLabel;
    @FXML private Button postButton;
    @FXML private Label formMessage;
    @FXML private Label locationLabel;

    private final AppContext ctx;
    private String currentLocation = "Unknown location";

    public CommentsController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        serverLabel.setText("Server: " + ctx.comments().address());
        usernameField.setText(ctx.prefs().get("username", ""));

        usernameField.setTextFormatter(maxLength(Comment.MAX_AUTHOR_LENGTH));
        commentInput.setTextFormatter(maxLength(Comment.MAX_TEXT_LENGTH));
        counterLabel.textProperty().bind(Bindings.createStringBinding(
                () -> commentInput.getLength() + " / " + Comment.MAX_TEXT_LENGTH,
                commentInput.lengthProperty()));

        // the red error box should only take up space when there's an error in it
        formMessage.visibleProperty().bind(formMessage.textProperty().isNotEmpty());
        formMessage.managedProperty().bind(formMessage.visibleProperty());

        setCurrentLocation(currentLocation);
        refresh();
    }

    public void setCurrentLocation(String location) {
        currentLocation = location;
        locationLabel.setText("Posting from " + location);
    }

    @FXML
    private void goBack() {
        ctx.navigator().showMain();
    }

    @FXML
    private void refresh() {
        showMessage("Loading comments...");
        Async.run(() -> ctx.comments().fetchAll(),
                this::showComments,
                error -> {
                    commentsContainer.getChildren().clear();
                    showMessage("Can't reach the comments server at " + ctx.comments().address() + ".\n\n"
                            + "Start it from the project folder with\nmvn compile exec:java");
                });
    }

    @FXML
    private void postComment() {
        String name = usernameField.getText().trim();
        String text = commentInput.getText().trim();
        if (name.isEmpty()) {
            showFormError("Pick a name first.");
            usernameField.requestFocus();
            return;
        }
        if (text.isEmpty()) {
            showFormError("Write something first.");
            commentInput.requestFocus();
            return;
        }

        ctx.prefs().put("username", name);
        postButton.setDisable(true);
        formMessage.setText("");

        Async.run(() -> {
                    ctx.comments().post(name, text, currentLocation);
                    return null;
                },
                ignored -> {
                    postButton.setDisable(false);
                    commentInput.clear();
                    refresh();
                },
                error -> {
                    postButton.setDisable(false);
                    showFormError("Couldn't post that: " + error.getMessage());
                });
    }

    private void showComments(List<Comment> comments) {
        commentsContainer.getChildren().clear();
        if (comments.isEmpty()) {
            showMessage("No comments yet. Be the first one!");
            return;
        }
        messageLabel.setVisible(false);
        // newest on top
        for (int i = comments.size() - 1; i >= 0; i--) {
            commentsContainer.getChildren().add(commentCard(comments.get(i)));
        }
    }

    private Node commentCard(Comment comment) {
        Label author = new Label("@" + comment.author());
        author.getStyleClass().add("comment-author");

        Label time = new Label(TIMESTAMP.format(Instant.ofEpochMilli(comment.postedAt()).atZone(ZoneId.systemDefault())));
        time.getStyleClass().add("comment-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label place = new Label(comment.location());
        place.getStyleClass().add("comment-location");

        HBox header = new HBox(10, author, time, spacer, place);
        header.setAlignment(Pos.CENTER_LEFT);

        Label text = new Label(comment.text());
        text.setWrapText(true);
        text.getStyleClass().add("comment-text");

        VBox card = new VBox(6, header, text);
        card.getStyleClass().add("comment-card");
        return card;
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
    }

    private void showFormError(String message) {
        formMessage.setText(message);
    }

    private static TextFormatter<String> maxLength(int max) {
        return new TextFormatter<>(change -> change.getControlNewText().length() <= max ? change : null);
    }
}
