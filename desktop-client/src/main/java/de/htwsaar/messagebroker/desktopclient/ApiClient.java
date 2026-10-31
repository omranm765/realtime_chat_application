package de.htwsaar.messagebroker.desktopclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.htwsaar.messagebroker.model.ChatMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public class ApiClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ApiClient(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getWebSocketUrl() {
        if (baseUrl.startsWith("https://")) {
            return "wss://" + baseUrl.substring("https://".length()) + "/ws";
        }
        if (baseUrl.startsWith("http://")) {
            return "ws://" + baseUrl.substring("http://".length()) + "/ws";
        }
        return baseUrl + "/ws";
    }

    public UserProfile registerUser(String displayName, String email, String password)
            throws IOException, InterruptedException {
        UserProfile user = new UserProfile();
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPassword(password);

        HttpRequest request = request("/api/users")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(user), StandardCharsets.UTF_8))
                .build();
        return readJson(request, UserProfile.class);
    }

    public UserProfile loginByEmail(String email) throws IOException, InterruptedException {
        HttpRequest request = request("/api/users/by-email?email=" + encodeQuery(email))
                .GET()
                .build();
        return readJson(request, UserProfile.class);
    }

    public void registerClient(String clientId) throws IOException, InterruptedException {
        HttpRequest request = request("/api/admin/clients")
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(clientId, StandardCharsets.UTF_8))
                .build();
        sendExpectSuccess(request);
    }

    public List<String> fetchEvents() throws IOException, InterruptedException {
        HttpRequest request = request("/api/admin/events").GET().build();
        return readJson(request, new TypeReference<>() {});
    }

    public void createEvent(String eventName) throws IOException, InterruptedException {
        HttpRequest request = request("/api/admin/events/" + encodeSegment(eventName))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        sendExpectSuccess(request);
    }

    public void deleteEvent(String eventName) throws IOException, InterruptedException {
        HttpRequest request = request("/api/admin/events/" + encodeSegment(eventName))
                .DELETE()
                .build();
        sendExpectSuccess(request);
    }

    public List<String> fetchClients() throws IOException, InterruptedException {
        HttpRequest request = request("/api/admin/clients").GET().build();
        return readJson(request, new TypeReference<>() {});
    }

    public List<UserProfile> fetchUsers() throws IOException, InterruptedException {
        HttpRequest request = request("/api/users").GET().build();
        return readJson(request, new TypeReference<>() {});
    }

    public List<String> fetchOnlineClients() throws IOException, InterruptedException {
        HttpRequest request = request("/api/admin/clients/online").GET().build();
        return readJson(request, new TypeReference<>() {});
    }

    public String fetchStatus() throws IOException, InterruptedException {
        HttpRequest request = request("/api/admin/status").GET().build();
        return sendExpectSuccess(request).body();
    }

    public Set<String> fetchSubscriptions(String clientId) throws IOException, InterruptedException {
        HttpRequest request = request("/api/clients/" + encodeSegment(clientId) + "/subscriptions")
                .GET()
                .build();
        return readJson(request, new TypeReference<>() {});
    }

    public void subscribe(String clientId, String eventName) throws IOException, InterruptedException {
        HttpRequest request = request("/api/clients/" + encodeSegment(clientId)
                + "/subscribe/" + encodeSegment(eventName))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        sendExpectSuccess(request);
    }

    public void unsubscribe(String clientId, String eventName) throws IOException, InterruptedException {
        HttpRequest request = request("/api/clients/" + encodeSegment(clientId)
                + "/unsubscribe/" + encodeSegment(eventName))
                .DELETE()
                .build();
        sendExpectSuccess(request);
    }

    public List<ChatMessage> fetchMessages(String eventName) throws IOException, InterruptedException {
        HttpRequest request = request("/api/messages/" + encodeSegment(eventName))
                .GET()
                .build();
        return readJson(request, new TypeReference<>() {});
    }

    public List<ChatMessage> fetchUndeliveredMessages(String clientId) throws IOException, InterruptedException {
        HttpRequest request = request("/api/messages/undelivered?clientId=" + encodeQuery(clientId))
                .GET()
                .build();
        return readJson(request, new TypeReference<>() {});
    }

    public void markDelivered(String clientId, List<Long> messageIds) throws IOException, InterruptedException {
        if (messageIds.isEmpty()) {
            return;
        }
        HttpRequest request = request("/api/messages/mark-delivered?clientId=" + encodeQuery(clientId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(messageIds), StandardCharsets.UTF_8))
                .build();
        sendExpectSuccess(request);
    }

    public void heartbeat(String clientId) throws IOException, InterruptedException {
        HttpRequest request = request("/api/heartbeat")
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(clientId, StandardCharsets.UTF_8))
                .build();
        sendExpectSuccess(request);
    }

    public void markOffline(String clientId) throws IOException, InterruptedException {
        HttpRequest request = request("/api/admin/heartbeat/offline")
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(clientId, StandardCharsets.UTF_8))
                .build();
        sendExpectSuccess(request);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10));
    }

    private <T> T readJson(HttpRequest request, Class<T> type) throws IOException, InterruptedException {
        HttpResponse<String> response = sendExpectSuccess(request);
        return mapper.readValue(response.body(), type);
    }

    private <T> T readJson(HttpRequest request, TypeReference<T> type) throws IOException, InterruptedException {
        HttpResponse<String> response = sendExpectSuccess(request);
        return mapper.readValue(response.body(), type);
    }

    private HttpResponse<String> sendExpectSuccess(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            String body = response.body() == null || response.body().isBlank()
                    ? "keine Antwortdetails"
                    : response.body();
            throw new IOException("HTTP " + status + ": " + body);
        }
        return response;
    }

    private static String normalizeBaseUrl(String url) {
        String trimmed = url == null ? "" : url.trim();
        if (trimmed.isEmpty()) {
            return "http://localhost:8080";
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
