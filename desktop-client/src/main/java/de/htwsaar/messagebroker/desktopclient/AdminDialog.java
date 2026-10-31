package de.htwsaar.messagebroker.desktopclient;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AdminDialog {
    private final ApiClient apiClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "desktop-admin-dialog");
        thread.setDaemon(true);
        return thread;
    });

    private final ObservableList<String> events = FXCollections.observableArrayList();
    private final ObservableList<String> clients = FXCollections.observableArrayList();
    private final ObservableList<String> onlineClients = FXCollections.observableArrayList();
    private final ObservableList<UserProfile> users = FXCollections.observableArrayList();

    private Stage stage;
    private Label statusLabel;
    private Label systemStatusLabel;
    private TextField eventField;
    private ListView<String> eventList;
    private ListView<String> clientList;
    private ListView<String> onlineClientList;
    private ListView<UserProfile> userList;

    public AdminDialog(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void show(Stage owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("MessageBroker Admin");
        stage.setMinWidth(760);
        stage.setMinHeight(600);
        stage.setOnCloseRequest(event -> executor.shutdownNow());

        VBox root = new VBox(16);
        root.getStyleClass().addAll("app-root", "admin-root");
        root.setPadding(new Insets(22));

        Label title = new Label("Admin-Konsole");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Events, Clients, Benutzer und Systemstatus verwalten.");
        subtitle.getStyleClass().add("app-subtitle");

        systemStatusLabel = new Label("Status wird geladen...");
        systemStatusLabel.getStyleClass().add("metric");

        Button refreshButton = button("Alles aktualisieren", "button-primary");
        refreshButton.setOnAction(event -> refreshAll());

        HBox header = new HBox(16, new VBox(4, title, subtitle, systemStatusLabel), refreshButton);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("admin-tabs");
        tabs.getTabs().addAll(eventsTab(), clientsTab(), usersTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        statusLabel = new Label("Bereit.");
        statusLabel.getStyleClass().addAll("status-line", "status-muted");

        root.getChildren().addAll(header, tabs, statusLabel);

        Scene scene = new Scene(root, 840, 680);
        var stylesheet = getClass().getResource("/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setScene(scene);
        stage.show();
        refreshAll();
    }

    private Tab eventsTab() {
        eventField = new TextField();
        eventField.setPromptText("Event-Name");

        Button createButton = button("Event anlegen", "button-primary");
        createButton.setOnAction(event -> createEvent());

        Button deleteButton = button("Ausgewaehltes Event loeschen", "button-danger");
        deleteButton.setOnAction(event -> deleteEvent());

        Button refreshButton = button("Events laden", "button-ghost");
        refreshButton.setOnAction(event -> refreshEvents());

        eventList = new ListView<>(events);
        eventList.getStyleClass().add("clean-list");
        eventList.setPlaceholder(new Label("Keine Events vorhanden"));
        VBox.setVgrow(eventList, Priority.ALWAYS);

        HBox form = new HBox(10, eventField, createButton);
        HBox.setHgrow(eventField, Priority.ALWAYS);

        HBox actions = new HBox(10, refreshButton, deleteButton);

        VBox content = tabContent(form, eventList, actions);
        return new Tab("Events", content);
    }

    private Tab clientsTab() {
        clientList = new ListView<>(clients);
        clientList.getStyleClass().add("clean-list");
        clientList.setPlaceholder(new Label("Keine Clients registriert"));
        VBox.setVgrow(clientList, Priority.ALWAYS);

        onlineClientList = new ListView<>(onlineClients);
        onlineClientList.getStyleClass().add("clean-list");
        onlineClientList.setPlaceholder(new Label("Keine Online-Clients"));
        VBox.setVgrow(onlineClientList, Priority.ALWAYS);

        Button refreshButton = button("Clients laden", "button-primary");
        refreshButton.setOnAction(event -> refreshClients());

        Button markOfflineButton = button("Online-Client offline setzen", "button-danger");
        markOfflineButton.setOnAction(event -> markSelectedClientOffline());

        VBox registered = card("Registrierte Clients", clientList);
        VBox online = card("Online-Clients", onlineClientList, markOfflineButton);
        HBox lists = new HBox(14, registered, online);
        HBox.setHgrow(registered, Priority.ALWAYS);
        HBox.setHgrow(online, Priority.ALWAYS);

        VBox content = tabContent(lists, refreshButton);
        return new Tab("Clients", content);
    }

    private Tab usersTab() {
        userList = new ListView<>(users);
        userList.getStyleClass().add("clean-list");
        userList.setPlaceholder(new Label("Keine Benutzer vorhanden"));
        userList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(UserProfile user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getId() + " | " + user.getDisplayName() + " | " + user.getEmail());
                }
            }
        });
        VBox.setVgrow(userList, Priority.ALWAYS);

        Button refreshButton = button("Benutzer laden", "button-primary");
        refreshButton.setOnAction(event -> refreshUsers());

        VBox content = tabContent(userList, refreshButton);
        return new Tab("Benutzer", content);
    }

    private VBox tabContent(javafx.scene.Node... nodes) {
        VBox content = new VBox(12);
        content.getStyleClass().add("admin-tab-content");
        content.getChildren().addAll(nodes);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("page-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox wrapper = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return wrapper;
    }

    private VBox card(String title, javafx.scene.Node... nodes) {
        Label label = new Label(title);
        label.getStyleClass().add("card-title");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.getChildren().add(label);
        card.getChildren().addAll(nodes);
        return card;
    }

    private Button button(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("app-button", styleClass);
        return button;
    }

    private void refreshAll() {
        runAsync(() -> new AdminSnapshot(
                sorted(apiClient.fetchEvents()),
                sorted(apiClient.fetchClients()),
                sorted(apiClient.fetchOnlineClients()),
                apiClient.fetchUsers(),
                apiClient.fetchStatus()
        ), snapshot -> {
            events.setAll(snapshot.events());
            clients.setAll(snapshot.clients());
            onlineClients.setAll(snapshot.onlineClients());
            users.setAll(snapshot.users());
            systemStatusLabel.setText(snapshot.status());
            showStatus("Admin-Daten aktualisiert.", false);
        }, error -> showStatus("Admin-Daten konnten nicht geladen werden: " + error.getMessage(), true));
    }

    private void refreshEvents() {
        runAsync(() -> sorted(apiClient.fetchEvents()), loaded -> {
            events.setAll(loaded);
            showStatus("Events geladen: " + loaded.size(), false);
        }, error -> showStatus("Events konnten nicht geladen werden: " + error.getMessage(), true));
    }

    private void refreshClients() {
        runAsync(() -> new ClientSnapshot(
                sorted(apiClient.fetchClients()),
                sorted(apiClient.fetchOnlineClients()),
                apiClient.fetchStatus()
        ), snapshot -> {
            clients.setAll(snapshot.clients());
            onlineClients.setAll(snapshot.onlineClients());
            systemStatusLabel.setText(snapshot.status());
            showStatus("Clients geladen.", false);
        }, error -> showStatus("Clients konnten nicht geladen werden: " + error.getMessage(), true));
    }

    private void refreshUsers() {
        runAsync(apiClient::fetchUsers, loaded -> {
            users.setAll(loaded);
            showStatus("Benutzer geladen: " + loaded.size(), false);
        }, error -> showStatus("Benutzer konnten nicht geladen werden: " + error.getMessage(), true));
    }

    private void createEvent() {
        String eventName = eventField.getText().trim();
        if (eventName.isBlank()) {
            showStatus("Event-Name darf nicht leer sein.", true);
            return;
        }

        runAsync(() -> {
            apiClient.createEvent(eventName);
            return eventName;
        }, created -> {
            eventField.clear();
            refreshEvents();
            showStatus("Event angelegt: " + created, false);
        }, error -> showStatus("Event konnte nicht angelegt werden: " + error.getMessage(), true));
    }

    private void deleteEvent() {
        String eventName = eventList.getSelectionModel().getSelectedItem();
        if (eventName == null) {
            showStatus("Bitte ein Event auswaehlen.", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Event loeschen");
        confirm.setHeaderText("Event '" + eventName + "' loeschen?");
        confirm.setContentText("Das Backend loescht dabei auch die Nachrichten dieses Events.");
        if (confirm.showAndWait().isEmpty() || confirm.getResult().getButtonData().isCancelButton()) {
            return;
        }

        runAsync(() -> {
            apiClient.deleteEvent(eventName);
            return eventName;
        }, deleted -> {
            events.remove(deleted);
            showStatus("Event geloescht: " + deleted, false);
        }, error -> showStatus("Event konnte nicht geloescht werden: " + error.getMessage(), true));
    }

    private void markSelectedClientOffline() {
        String clientId = onlineClientList.getSelectionModel().getSelectedItem();
        if (clientId == null) {
            showStatus("Bitte einen Online-Client auswaehlen.", true);
            return;
        }

        runAsync(() -> {
            apiClient.markOffline(clientId);
            return clientId;
        }, offline -> {
            onlineClients.remove(offline);
            showStatus("Client offline gesetzt: " + offline, false);
        }, error -> showStatus("Client konnte nicht offline gesetzt werden: " + error.getMessage(), true));
    }

    private void showStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("status-muted", "status-error", "status-ok");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-ok");
    }

    private <T> void runAsync(Callable<T> task, Consumer<T> success, Consumer<Throwable> failure) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        }, executor).whenComplete((result, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                failure.accept(unwrap(throwable));
            } else {
                success.accept(result);
            }
        }));
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static List<String> sorted(List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private record AdminSnapshot(
            List<String> events,
            List<String> clients,
            List<String> onlineClients,
            List<UserProfile> users,
            String status
    ) {
    }

    private record ClientSnapshot(List<String> clients, List<String> onlineClients, String status) {
    }
}
