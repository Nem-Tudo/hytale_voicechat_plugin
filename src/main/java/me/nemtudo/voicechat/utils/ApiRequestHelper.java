package me.nemtudo.voicechat.utils;

import com.hypixel.hytale.logger.HytaleLogger;
import me.nemtudo.voicechat.VoiceChat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ApiRequestHelper {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final VoiceChat plugin;

    public ApiRequestHelper(VoiceChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Faz uma requisição à API com autenticação automática.
     *
     * @param method Method HTTP (GET, POST, PUT, DELETE, etc)
     * @param url    URL do endpoint (relativa ou absoluta)
     * @param body   Objeto a ser serializado como JSON (pode ser null para GET/DELETE)
     * @return CompletableFuture com a resposta HTTP
     */
    public CompletableFuture<HttpResponse<String>> request(String method, String url, Object body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Adiciona o apiQuerySuffix detectando se já existe query string
                String finalUrl = plugin.config.get().getApiBaseUrl() + url + (url.contains("?") ? "&" : "?") + plugin.apiQuerySuffix;

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(finalUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + plugin.config.get().getServerToken())
                        .timeout(Duration.ofSeconds(5));

                // Define o method e body
                HttpRequest.BodyPublisher bodyPublisher = body != null
                        ? HttpRequest.BodyPublishers.ofString(plugin.gson.toJson(body))
                        : HttpRequest.BodyPublishers.noBody();

                switch (method.toUpperCase()) {
                    case "GET" -> requestBuilder.GET();
                    case "POST" -> requestBuilder.POST(bodyPublisher);
                    case "PUT" -> requestBuilder.PUT(bodyPublisher);
                    case "DELETE" -> requestBuilder.DELETE();
                    case "PATCH" -> requestBuilder.method("PATCH", bodyPublisher);
                    default -> throw new IllegalArgumentException("Invalid HTTP method: " + method);
                }

                HttpRequest request = requestBuilder.build();

                return plugin.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            } catch (Exception e) {
                LOGGER.atSevere().log("API request failed [" + method + " " + url + "]", e);
                throw new RuntimeException("API request failed", e);
            }
        });
    }

    /**
     * Versão síncrona da request (bloqueia a thread atual).
     */
    public HttpResponse<String> requestSync(String method, String url, Object body) {
        try {
            String finalUrl = plugin.config.get().getApiBaseUrl() + url + (url.contains("?") ? "&" : "?") + plugin.apiQuerySuffix.substring(1);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(finalUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + plugin.config.get().getServerToken())
                    .timeout(Duration.ofSeconds(5));

            HttpRequest.BodyPublisher bodyPublisher = body != null
                    ? HttpRequest.BodyPublishers.ofString(plugin.gson.toJson(body))
                    : HttpRequest.BodyPublishers.noBody();

            switch (method.toUpperCase()) {
                case "GET" -> requestBuilder.GET();
                case "POST" -> requestBuilder.POST(bodyPublisher);
                case "PUT" -> requestBuilder.PUT(bodyPublisher);
                case "DELETE" -> requestBuilder.DELETE();
                case "PATCH" -> requestBuilder.method("PATCH", bodyPublisher);
                default -> throw new IllegalArgumentException("Invalid HTTP method: " + method);
            }

            return plugin.httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            LOGGER.atSevere().log("API request failed [" + method + " " + url + "]", e);
            throw new RuntimeException("API request failed", e);
        }
    }
}