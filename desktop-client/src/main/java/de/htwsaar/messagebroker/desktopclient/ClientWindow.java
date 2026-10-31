package de.htwsaar.messagebroker.desktopclient;

import de.htwsaar.messagebroker.model.ChatMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ClientWindow {
    private static final Set<ClientWindow> OPEN_WINDOWS = ConcurrentHashMap.newKeySet();
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final ExecutorService ioExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "desktop-client-io");
        thread.setDaemon(true);
        return thread;
    });

    private final ObservableList<String> events = FXCollections.observableArrayList();
    private final ObservableList<String> subscriptions = FXCollections.observableArrayList();
    private final ObservableList<String> onlineClients = FXCollections.observableArrayList();
    private final Map<String, ObservableList<ChatMessage>> messagesByEvent = new HashMap<>();

    private ApiClient apiClient;
    private StompChatClient stompClient;
    private UserProfile currentUser;
    private ScheduledExecutorService heartbeatExecutor;

    private String activeEvent;

    private TextField serverField;
    private TextField displayNameField;
    private TextField emailField;
    private PasswordField passwordField;
    private TextField eventNameField;
    private TextField messageField;

    private Label userBadge;
    private Label connectionBadge;
    private Label statusLabel;
    private Label systemStatusLabel;
    private Label activeEventTitle;
    private Label activeEventMeta;

    private ListView<String> eventList;
    private ListView<String> subscriptionList;
    private ListView<String> onlineList;
    private VBox messageBox;
    private ScrollPane messageScroll;

    private Button loginButton;
    private Button registerButton;
    private Button refreshEventsButton;
    private Button createEventButton;
    private Button deleteEventButton;
    private Button subscribeButton;
    private Button unsubscribeButton;
    private Button refreshOnlineButton;
    private Button loadHistoryButton;
    private Button loadOfflineButton;
    private Button sendButton;
    private Button newWindowButton;
    private Button adminButton;

    private boolean closed;
    private Stage stage;

    public void show(Stage stage) {
        OPEN_WINDOWS.add(this);
        this.stage = stage;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        VBox page = new VBox(18);
        page.getStyleClass().add("page");
        page.getChildren().addAll(createHeader(), createBody());

        ScrollPane pageScroll = new ScrollPane(page);
        pageScroll.getStyleClass().add("page-scroll");
        pageScroll.setFitToWidth(true);
        pageScroll.setPannable(true);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        root.setCenter(pageScroll);

        statusLabel = new Label("Backend starten und anmelden.");
        statusLabel.getStyleClass().addAll("status-line", "status-muted");
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 1240, 780);
        var stylesheet = getClass().getResource("/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("MessageBroker Desktop Client");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            shutdown();
            OPEN_WINDOWS.remove(this);
        });
        stage.show();

        updateUiState();
    }

    private Node createHeader() {
        Label eyebrow = new Label("Realtime Chat");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("MessageBroker");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("JavaFX Client fuer Events, Subscriptions, Offline Messages und Live-STOMP.");
        subtitle.getStyleClass().add("app-subtitle");

        VBox titleBlock = new VBox(4, eyebrow, title, subtitle);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        userBadge = new Label("Nicht angemeldet");
        userBadge.getStyleClass().addAll("badge", "badge-muted");

        connectionBadge = new Label("Offline");
        connectionBadge.getStyleClass().addAll("badge", "badge-danger");

        newWindowButton = button("Neue Instanz", "button-ghost");
        newWindowButton.setOnAction(event -> new ClientWindow().show(new Stage()));

        adminButton = button("Admin", "button-secondary");
        adminButton.setOnAction(event -> openAdminDialog());

        HBox badges = new HBox(10, adminButton, newWindowButton, userBadge, connectionBadge);
        badges.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(20, titleBlock, badges);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node createBody() {
        VBox sidebar = new VBox(16);
        sidebar.getStyleClass().add("sidebar");
        sidebar.getChildren().addAll(createSessionCard(), createEventsCard(), createOnlineCard());

        VBox chat = createChatPanel();
        HBox.setHgrow(chat, Priority.ALWAYS);

        HBox body = new HBox(18, sidebar, chat);
        body.getStyleClass().add("body");
        return body;
    }

    private Node createSessionCard() {
        serverField = new TextField("http://localhost:8080");
        serverField.setPromptText("Backend URL");

        displayNameField = new TextField();
        displayNameField.setPromptText("Anzeigename fuer Registrierung");

        emailField = new TextField();
        emailField.setPromptText("E-Mail / Client-ID");

        passwordField = new PasswordField();
        passwordField.setPromptText("Passwort fuer Registrierung");

        loginButton = button("Login", "button-primary");
        loginButton.setOnAction(event -> authenticate(false));

        registerButton = button("Registrieren", "button-secondary");
        registerButton.setOnAction(event -> authenticate(true));

        HBox actions = new HBox(10, loginButton, registerButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("Login erfolgt ueber die vorhandene API per E-Mail.");
        hint.getStyleClass().add("hint");

        return card("Session", serverField, displayNameField, emailField, passwordField, actions, hint);
    }

    private Node createEventsCard() {
        eventList = new ListView<>(events);
        eventList.getStyleClass().add("clean-list");
        eventList.setPrefHeight(150);
        eventList.setPlaceholder(new Label("Keine Events gefunden"));
        eventList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("subscribed-cell");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                if (subscriptions.contains(item)) {
                    getStyleClass().add("subscribed-cell");
                }
            }
        });
        eventList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                setActiveEvent(newValue, true);
            }
        });

        subscriptions.addListener((ListChangeListener<String>) change -> eventList.refresh());

        subscriptionList = new ListView<>(subscriptions);
        subscriptionList.getStyleClass().add("clean-list");
        subscriptionList.setPrefHeight(95);
        subscriptionList.setPlaceholder(new Label("Noch keine Abos"));
        subscriptionList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                setActiveEvent(newValue, true);
            }
        });

        refreshEventsButton = button("Aktualisieren", "button-ghost");
        refreshEventsButton.setOnAction(event -> refreshEvents());

        createEventButton = button("Event anlegen", "button-primary");
        createEventButton.setOnAction(event -> createEvent());

        deleteEventButton = button("Loeschen", "button-danger");
        deleteEventButton.setOnAction(event -> deleteSelectedEvent());

        subscribeButton = button("Abonnieren", "button-secondary");
        subscribeButton.setOnAction(event -> subscribeSelectedEvent());

        unsubscribeButton = button("Abo entfernen", "button-ghost");
        unsubscribeButton.setOnAction(event -> unsubscribeSelectedEvent());

        eventNameField = new TextField();
        eventNameField.setPromptText("neues-event");

        HBox eventActions = new HBox(8, refreshEventsButton, createEventButton);
        HBox subscriptionActions = new HBox(8, subscribeButton, unsubscribeButton, deleteEventButton);

        return card("Events", eventNameField, eventActions, sectionLabel("Alle Events"), eventList,
                sectionLabel("Meine Abos"), subscriptionList, subscriptionActions);
    }

    private Node createOnlineCard() {
        systemStatusLabel = new Label("Status noch nicht geladen");
        systemStatusLabel.getStyleClass().add("metric");

        onlineList = new ListView<>(onlineClients);
        onlineList.getStyleClass().add("clean-list");
        onlineList.setPrefHeight(92);
        onlineList.setPlaceholder(new Label("Niemand online"));

        refreshOnlineButton = button("Online-Status laden", "button-ghost");
        refreshOnlineButton.setOnAction(event -> refreshOnlineClients());

        return card("System", systemStatusLabel, onlineList, refreshOnlineButton);
    }

    private VBox createChatPanel() {
        activeEventTitle = new Label("Kein Event ausgewaehlt");
        activeEventTitle.getStyleClass().add("chat-title");

        activeEventMeta = new Label("Waehle ein Event oder lege eins an.");
        activeEventMeta.getStyleClass().add("chat-meta");

        VBox activeBlock = new VBox(4, activeEventTitle, activeEventMeta);
        HBox.setHgrow(activeBlock, Priority.ALWAYS);

        loadHistoryButton = button("Verlauf laden", "button-ghost");
        loadHistoryButton.setOnAction(event -> {
            if (activeEvent != null) {
                loadHistory(activeEvent);
            }
        });

        loadOfflineButton = button("Verpasste laden", "button-secondary");
        loadOfflineButton.setOnAction(event -> loadUndeliveredMessages());

        HBox chatHeader = new HBox(12, activeBlock, loadHistoryButton, loadOfflineButton);
        chatHeader.setAlignment(Pos.CENTER_LEFT);

        messageBox = new VBox(12);
        messageBox.getStyleClass().add("message-box");

        messageScroll = new ScrollPane(messageBox);
        messageScroll.setFitToWidth(true);
        messageScroll.setPannable(true);
        messageScroll.setPrefViewportHeight(420);
        messageScroll.setMinViewportHeight(260);
        messageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScroll.getStyleClass().add("message-scroll");
        VBox.setVgrow(messageScroll, Priority.ALWAYS);

        messageField = new TextField();
        messageField.setPromptText("Nachricht schreiben...");
        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });
        HBox.setHgrow(messageField, Priority.ALWAYS);

        sendButton = button("Senden", "button-primary");
        sendButton.setOnAction(event -> sendMessage());

        HBox composer = new HBox(10, messageField, sendButton);
        composer.getStyleClass().add("composer");
        composer.setAlignment(Pos.CENTER);

        VBox chat = new VBox(14, chatHeader, messageScroll, composer);
        chat.getStyleClass().addAll("card", "chat-card");
        renderActiveEvent();
        return chat;
    }

    private VBox card(String title, Node... children) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.getChildren().add(titleLabel);
        card.getChildren().addAll(children);
        return card;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-label");
        return label;
    }

    private Button button(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("app-button", styleClass);
        return button;
    }

    private void authenticate(boolean createUser) {
        String baseUrl = serverField.getText().trim();
        String email = emailField.getText().trim();
        String displayName = displayNameField.getText().trim();
        String password = passwordField.getText();

        if (email.isBlank()) {
            showStatus("E-Mail ist erforderlich.", true);
            return;
        }
        if (createUser && displayName.isBlank()) {
            showStatus("Anzeigename ist fuer Registrierung erforderlich.", true);
            return;
        }

        setBusy(true);
        showStatus(createUser ? "Registriere Benutzer..." : "Melde Benutzer an...", false);

        runAsync(() -> {
            ApiClient api = new ApiClient(baseUrl);
            UserProfile user = createUser
                    ? api.registerUser(displayName, email, password)
                    : api.loginByEmail(email);
            api.registerClient(user.getEmail());
            return new SessionStart(api, user);
        }, session -> {
            apiClient = session.apiClient();
            currentUser = session.user();
            userBadge.setText(currentUser.getDisplayName() + " | " + currentUser.getEmail());
            userBadge.getStyleClass().removeAll("badge-muted", "badge-danger", "badge-live");
            userBadge.getStyleClass().add("badge-live");
            setBusy(false);
            updateUiState();
            connectWebSocket();
            refreshEvents();
            refreshSubscriptions(true);
            refreshOnlineClients();
            startHeartbeat();
            showStatus("Angemeldet als " + currentUser.getDisplayName() + ".", false);
        }, error -> {
            setBusy(false);
            showStatus("Anmeldung fehlgeschlagen: " + error.getMessage(), true);
        });
    }

    private void openAdminDialog() {
        String baseUrl = serverField.getText().trim();
        ApiClient adminApi = new ApiClient(baseUrl);
        new AdminDialog(adminApi).show(stage);
    }

    private void connectWebSocket() {
        if (!requireSession()) {
            return;
        }

        StompChatClient candidate = new StompChatClient(
                message -> Platform.runLater(() -> handleIncomingMessage(message)),
                status -> Platform.runLater(() -> showStatus(status, false))
        );

        setConnectionBadge("Verbinde...", false);
        runAsync(() -> {
            candidate.connect(apiClient.getWebSocketUrl());
            return candidate;
        }, client -> {
            if (stompClient != null) {
                stompClient.disconnect();
            }
            stompClient = client;
            subscriptions.forEach(stompClient::subscribe);
            setConnectionBadge("Live", true);
            updateUiState();
        }, error -> {
            setConnectionBadge("Offline", false);
            showStatus("WebSocket-Verbindung fehlgeschlagen: " + error.getMessage(), true);
            updateUiState();
        });
    }

    private void refreshEvents() {
        if (!requireSession()) {
            return;
        }

        runAsync(() -> sorted(apiClient.fetchEvents()), loadedEvents -> {
            events.setAll(loadedEvents);
            if (activeEvent == null && !loadedEvents.isEmpty()) {
                eventList.getSelectionModel().select(0);
                setActiveEvent(loadedEvents.get(0), true);
            } else if (activeEvent != null && !loadedEvents.contains(activeEvent)) {
                setActiveEvent(null, false);
            }
            showStatus("Events geladen: " + loadedEvents.size(), false);
            updateUiState();
        }, error -> showStatus("Events konnten nicht geladen werden: " + error.getMessage(), true));
    }

    private void refreshSubscriptions(boolean loadMissedAfterRefresh) {
        if (!requireSession()) {
            return;
        }

        runAsync(() -> sorted(new ArrayList<>(apiClient.fetchSubscriptions(currentUser.getEmail()))), loaded -> {
            subscriptions.setAll(loaded);
            if (stompClient != null && stompClient.isConnected()) {
                loaded.forEach(stompClient::subscribe);
            }
            if (activeEvent == null && !loaded.isEmpty()) {
                setActiveEvent(loaded.get(0), true);
            }
            if (loadMissedAfterRefresh) {
                loadUndeliveredMessages();
            }
            updateUiState();
        }, error -> showStatus("Abos konnten nicht geladen werden: " + error.getMessage(), true));
    }

    private void refreshOnlineClients() {
        if (!requireSession()) {
            return;
        }

        runAsync(() -> new OnlineSnapshot(
                sorted(apiClient.fetchOnlineClients()),
                apiClient.fetchStatus()
        ), snapshot -> {
            onlineClients.setAll(snapshot.onlineClients());
            systemStatusLabel.setText(snapshot.status());
        }, error -> showStatus("Online-Status konnte nicht geladen werden: " + error.getMessage(), true));
    }

    private void createEvent() {
        if (!requireSession()) {
            return;
        }

        String eventName = eventNameField.getText().trim();
        if (eventName.isBlank()) {
            showStatus("Event-Name darf nicht leer sein.", true);
            return;
        }

        runAsync(() -> {
            apiClient.createEvent(eventName);
            return eventName;
        }, created -> {
            eventNameField.clear();
            refreshEvents();
            setActiveEvent(created, true);
            showStatus("Event angelegt: " + created, false);
        }, error -> showStatus("Event konnte nicht angelegt werden: " + error.getMessage(), true));
    }

    private void deleteSelectedEvent() {
        if (!requireSession()) {
            return;
        }

        String eventName = selectedEventName();
        if (eventName == null) {
            showStatus("Bitte zuerst ein Event auswaehlen.", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Event loeschen");
        confirm.setHeaderText("Event '" + eventName + "' loeschen?");
        confirm.setContentText("Das Backend loescht dabei auch die zugehoerigen Nachrichten.");
        if (confirm.showAndWait().isEmpty() || confirm.getResult().getButtonData().isCancelButton()) {
            return;
        }

        runAsync(() -> {
            apiClient.deleteEvent(eventName);
            return eventName;
        }, deleted -> {
            subscriptions.remove(deleted);
            if (stompClient != null) {
                stompClient.unsubscribe(deleted);
            }
            messagesByEvent.remove(deleted);
            if (Objects.equals(activeEvent, deleted)) {
                setActiveEvent(null, false);
            }
            refreshEvents();
            refreshSubscriptions(false);
            showStatus("Event geloescht: " + deleted, false);
        }, error -> showStatus("Event konnte nicht geloescht werden: " + error.getMessage(), true));
    }

    private void subscribeSelectedEvent() {
        if (!requireSession()) {
            return;
        }

        String eventName = selectedEventName();
        if (eventName == null) {
            showStatus("Bitte zuerst ein Event auswaehlen.", true);
            return;
        }
        subscribeToEvent(eventName, true);
    }

    private void subscribeToEvent(String eventName, boolean loadHistoryAfterSubscribe) {
        runAsync(() -> {
            apiClient.subscribe(currentUser.getEmail(), eventName);
            return eventName;
        }, subscribed -> {
            if (!subscriptions.contains(subscribed)) {
                subscriptions.add(subscribed);
                subscriptions.sort(Comparator.naturalOrder());
            }
            if (stompClient != null && stompClient.isConnected()) {
                stompClient.subscribe(subscribed);
            }
            setActiveEvent(subscribed, loadHistoryAfterSubscribe);
            showStatus("Abo aktiv: " + subscribed, false);
            updateUiState();
        }, error -> showStatus("Abo konnte nicht erstellt werden: " + error.getMessage(), true));
    }

    private void unsubscribeSelectedEvent() {
        if (!requireSession()) {
            return;
        }

        String eventName = subscriptionList.getSelectionModel().getSelectedItem();
        if (eventName == null) {
            eventName = activeEvent;
        }
        if (eventName == null) {
            showStatus("Bitte zuerst ein Abo oder Event auswaehlen.", true);
            return;
        }

        String finalEventName = eventName;
        runAsync(() -> {
            apiClient.unsubscribe(currentUser.getEmail(), finalEventName);
            return finalEventName;
        }, unsubscribed -> {
            subscriptions.remove(unsubscribed);
            if (stompClient != null) {
                stompClient.unsubscribe(unsubscribed);
            }
            showStatus("Abo entfernt: " + unsubscribed, false);
            updateUiState();
        }, error -> showStatus("Abo konnte nicht entfernt werden: " + error.getMessage(), true));
    }

    private void loadHistory(String eventName) {
        if (!requireSession() || eventName == null) {
            return;
        }

        runAsync(() -> apiClient.fetchMessages(eventName), messages -> {
            mergeMessages(eventName, messages);
            renderActiveEvent();
            showStatus("Verlauf geladen: " + messages.size() + " Nachrichten.", false);
        }, error -> showStatus("Verlauf konnte nicht geladen werden: " + error.getMessage(), true));
    }

    private void loadUndeliveredMessages() {
        if (!requireSession()) {
            return;
        }

        runAsync(() -> apiClient.fetchUndeliveredMessages(currentUser.getEmail()), messages -> {
            Map<String, List<ChatMessage>> grouped = messages.stream()
                    .filter(message -> message.getEventType() != null)
                    .collect(Collectors.groupingBy(ChatMessage::getEventType));
            grouped.forEach(this::mergeMessages);
            renderActiveEvent();

            List<Long> ids = messages.stream()
                    .map(ChatMessage::getId)
                    .filter(Objects::nonNull)
                    .toList();
            markDeliveredAsync(ids);

            if (!messages.isEmpty()) {
                showStatus("Verpasste Nachrichten geladen: " + messages.size(), false);
            }
        }, error -> showStatus("Verpasste Nachrichten konnten nicht geladen werden: " + error.getMessage(), true));
    }

    private void sendMessage() {
        if (!requireSession()) {
            return;
        }
        if (activeEvent == null) {
            showStatus("Bitte zuerst ein Event auswaehlen.", true);
            return;
        }
        if (stompClient == null || !stompClient.isConnected()) {
            showStatus("WebSocket ist nicht verbunden.", true);
            return;
        }

        String content = messageField.getText().trim();
        if (content.isBlank()) {
            return;
        }

        String eventName = activeEvent;
        boolean needsSubscription = !subscriptions.contains(eventName);
        ChatMessage message = new ChatMessage(
                currentUser.getId(),
                currentUser.getDisplayName(),
                eventName,
                content,
                System.currentTimeMillis()
        );

        runAsync(() -> {
            if (needsSubscription) {
                apiClient.subscribe(currentUser.getEmail(), eventName);
                stompClient.subscribe(eventName);
            }
            stompClient.send(message);
            return needsSubscription;
        }, subscribedBeforeSend -> {
            if (subscribedBeforeSend && !subscriptions.contains(eventName)) {
                subscriptions.add(eventName);
                subscriptions.sort(Comparator.naturalOrder());
            }
            messageField.clear();
            showStatus("Nachricht gesendet.", false);
            updateUiState();
        }, error -> showStatus("Nachricht konnte nicht gesendet werden: " + error.getMessage(), true));
    }

    private void handleIncomingMessage(ChatMessage message) {
        String eventName = message.getEventType();
        if (eventName == null || eventName.isBlank()) {
            return;
        }

        mergeMessages(eventName, List.of(message));
        if (Objects.equals(activeEvent, eventName)) {
            renderActiveEvent();
        }
        if (!Objects.equals(currentUser.getId(), message.getSenderId())) {
            showStatus("Neue Nachricht in " + eventName + ".", false);
        }

        if (message.getId() != null) {
            markDeliveredAsync(List.of(message.getId()));
        }
    }

    private void setActiveEvent(String eventName, boolean loadHistory) {
        activeEvent = eventName;
        if (eventName != null && events.contains(eventName)
                && !Objects.equals(eventList.getSelectionModel().getSelectedItem(), eventName)) {
            eventList.getSelectionModel().select(eventName);
        }
        if (eventName != null && subscriptions.contains(eventName)
                && !Objects.equals(subscriptionList.getSelectionModel().getSelectedItem(), eventName)) {
            subscriptionList.getSelectionModel().select(eventName);
        }
        renderActiveEvent();
        updateUiState();
        if (loadHistory && eventName != null && currentUser != null) {
            loadHistory(eventName);
        }
    }

    private void renderActiveEvent() {
        messageBox.getChildren().clear();

        if (activeEvent == null) {
            activeEventTitle.setText("Kein Event ausgewaehlt");
            activeEventMeta.setText("Waehle links ein Event aus oder lege ein neues an.");
            messageBox.getChildren().add(emptyState("Kein Chat aktiv"));
            return;
        }

        ObservableList<ChatMessage> messages = messagesByEvent.getOrDefault(activeEvent, FXCollections.observableArrayList());
        activeEventTitle.setText("# " + activeEvent);
        activeEventMeta.setText(messages.size() + " Nachrichten | "
                + (subscriptions.contains(activeEvent) ? "Live-Abo aktiv" : "nicht abonniert"));

        if (messages.isEmpty()) {
            messageBox.getChildren().add(emptyState("Noch keine Nachrichten in diesem Event."));
        } else {
            for (ChatMessage message : messages) {
                messageBox.getChildren().add(messageNode(message));
            }
        }

        Platform.runLater(() -> messageScroll.setVvalue(1.0));
    }

    private Node emptyState(String text) {
        StackPane pane = new StackPane(new Label(text));
        pane.getStyleClass().add("empty-state");
        pane.setMinHeight(220);
        return pane;
    }

    private Node messageNode(ChatMessage message) {
        boolean own = currentUser != null && Objects.equals(currentUser.getId(), message.getSenderId());

        Label meta = new Label(senderName(message) + " | " + formatTime(message.getTimestamp()));
        meta.getStyleClass().add("message-meta");

        Label content = new Label(message.getContent());
        content.setWrapText(true);
        content.setMaxWidth(560);
        content.getStyleClass().add("message-content");

        VBox bubble = new VBox(6, meta, content);
        bubble.getStyleClass().addAll("message-bubble", own ? "message-own" : "message-incoming");

        HBox row = new HBox(bubble);
        row.getStyleClass().add("message-row");
        row.setAlignment(own ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    private void mergeMessages(String eventName, List<ChatMessage> incoming) {
        ObservableList<ChatMessage> messages = messagesByEvent.computeIfAbsent(
                eventName,
                ignored -> FXCollections.observableArrayList()
        );
        Set<Long> existingIds = messages.stream()
                .map(ChatMessage::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        for (ChatMessage message : incoming) {
            Long id = message.getId();
            if (id == null || !existingIds.contains(id)) {
                messages.add(message);
                if (id != null) {
                    existingIds.add(id);
                }
            }
        }

        messages.sort(Comparator
                .comparingLong(ChatMessage::getTimestamp)
                .thenComparing(message -> message.getId() == null ? Long.MAX_VALUE : message.getId()));
    }

    private void markDeliveredAsync(List<Long> messageIds) {
        if (apiClient == null || currentUser == null || messageIds.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                apiClient.markDelivered(currentUser.getEmail(), messageIds);
            } catch (Exception ignored) {
                // Delivery marking should not interrupt the visible chat flow.
            }
        }, ioExecutor);
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "desktop-client-heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (apiClient == null || currentUser == null) {
                return;
            }
            try {
                apiClient.heartbeat(currentUser.getEmail());
            } catch (Exception ignored) {
                Platform.runLater(() -> setConnectionBadge("REST offline", false));
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }
    }

    private void updateUiState() {
        boolean loggedIn = currentUser != null;
        boolean hasActiveEvent = activeEvent != null;

        refreshEventsButton.setDisable(!loggedIn);
        createEventButton.setDisable(!loggedIn);
        deleteEventButton.setDisable(!loggedIn || !hasActiveEvent);
        subscribeButton.setDisable(!loggedIn || !hasActiveEvent || subscriptions.contains(activeEvent));
        unsubscribeButton.setDisable(!loggedIn || !hasActiveEvent || !subscriptions.contains(activeEvent));
        refreshOnlineButton.setDisable(!loggedIn);
        loadHistoryButton.setDisable(!loggedIn || !hasActiveEvent);
        loadOfflineButton.setDisable(!loggedIn);
        sendButton.setDisable(!loggedIn || !hasActiveEvent);
        messageField.setDisable(!loggedIn || !hasActiveEvent);
        eventList.setDisable(!loggedIn);
        subscriptionList.setDisable(!loggedIn);
        onlineList.setDisable(!loggedIn);
        eventNameField.setDisable(!loggedIn);
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        registerButton.setDisable(busy);
        serverField.setDisable(busy);
    }

    private boolean requireSession() {
        if (apiClient == null || currentUser == null) {
            showStatus("Bitte zuerst anmelden.", true);
            return false;
        }
        return true;
    }

    private String selectedEventName() {
        String selected = eventList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            return selected;
        }
        String typed = eventNameField.getText().trim();
        if (!typed.isBlank()) {
            return typed;
        }
        return activeEvent;
    }

    private void setConnectionBadge(String text, boolean live) {
        connectionBadge.setText(text);
        connectionBadge.getStyleClass().removeAll("badge-muted", "badge-danger", "badge-live");
        connectionBadge.getStyleClass().add(live ? "badge-live" : "badge-danger");
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
        }, ioExecutor).whenComplete((result, throwable) -> Platform.runLater(() -> {
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

    private static String senderName(ChatMessage message) {
        if (message.getSenderDisplayName() != null && !message.getSenderDisplayName().isBlank()) {
            return message.getSenderDisplayName();
        }
        if (message.getSenderId() != null) {
            return "User " + message.getSenderId();
        }
        return "Unbekannt";
    }

    private static String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "--:--";
        }
        return TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    private void shutdown() {
        if (closed) {
            return;
        }
        closed = true;

        stopHeartbeat();

        if (stompClient != null) {
            stompClient.disconnect();
        }

        if (apiClient != null && currentUser != null) {
            try {
                apiClient.markOffline(currentUser.getEmail());
            } catch (Exception ignored) {
                // Closing the app should not block on backend availability.
            }
        }

        ioExecutor.shutdownNow();
    }

    public static void shutdownAll() {
        for (ClientWindow window : Set.copyOf(OPEN_WINDOWS)) {
            window.shutdown();
        }
        OPEN_WINDOWS.clear();
    }

    private record SessionStart(ApiClient apiClient, UserProfile user) {
    }

    private record OnlineSnapshot(List<String> onlineClients, String status) {
    }
}
