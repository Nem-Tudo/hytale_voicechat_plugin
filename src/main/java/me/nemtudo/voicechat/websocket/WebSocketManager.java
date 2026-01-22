package me.nemtudo.voicechat.websocket;

import com.hypixel.hytale.logger.HytaleLogger;
import io.socket.client.IO;
import io.socket.client.Socket;
import me.nemtudo.voicechat.VoiceChat;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class WebSocketManager {

    private final HytaleLogger LOGGER;

    private static final int MAX_RECONNECT_ATTEMPTS = 30;
    private static final long RECONNECT_DELAY = 2000;
    private static final long ERROR_DISPLAY_DELAY = 15000;
    private static final long PING_INTERVAL = 25000;
    private static final long PING_TIMEOUT = 60000;

    private final VoiceChat plugin;
    private Socket socket;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean disconnectForced = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    private final Map<String, Consumer<Object[]>> eventListeners = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean autoReconnect = true;

    public WebSocketManager(VoiceChat plugin) {
        this.plugin = plugin;
        this.LOGGER = plugin.getLogger();
    }

    /**
     * Conecta ao servidor WebSocket
     *
     * @param query Parâmetros de query string adicionais
     */
    public void connect(Map<String, String> query) {
        query.put("socketApiVersion", "2");
        query.put("server_token", plugin.config.get().getServerToken());
        query.put("client_type", "server");


        if (connecting.get() || connected.get()) {
            if (plugin.config.get().getLogWebsocketInfoInConsole())
                LOGGER.atInfo().log("[WebSocket] Already connected or connecting");
            return;
        }

        connecting.set(true);
        disconnectForced.set(false);
        reconnectAttempts.set(0);

        LOGGER.atInfo().log("[WebSocket] Starting connection...");

        try {
            String socketUrl = plugin.config.get().getWebsocketBaseUrl();

            IO.Options options = IO.Options.builder()
                    .setQuery(createQueryString(query))
                    .setTransports(new String[]{"websocket", "polling"})
                    .setUpgrade(true)
                    .setReconnection(true)
                    .setReconnectionDelay(RECONNECT_DELAY)
                    .setReconnectionAttempts(MAX_RECONNECT_ATTEMPTS)
                    .setTimeout(10000)
                    .setForceNew(false)
                    .build();

            socket = IO.socket(URI.create(socketUrl), options);

            setupEventHandlers();

            registerEvents();

            socket.connect();


            // Timeout para exibir erro se demorar muito
            scheduler.schedule(() -> {
                if (connecting.get() && !connected.get()) {
                    LOGGER.atWarning().log("[WebSocket] Connection time limit exceeded");
                    connecting.set(false);
                }
            }, ERROR_DISPLAY_DELAY, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            LOGGER.atSevere().log("[WebSocket] Error on create: " + e.getMessage());
            connecting.set(false);
        }
    }

    private void registerEvents() {
        WebSocketEventRegistry registry = new WebSocketEventRegistry(plugin, socket);
        registry.registerAllEvents();
    }

    /**
     * Conecta com query vazio
     */
    public void connect() {
        connect(new HashMap<>());
    }

    /**
     * Desconecta do servidor WebSocket
     */
    public void disconnect() {
        LOGGER.atInfo().log("[WebSocket] Manual disconnect");

        disconnectForced.set(true);
        autoReconnect = false;

        if (socket != null && socket.connected()) {
            socket.disconnect();
        }

        connected.set(false);
        connecting.set(false);
    }

    /**
     * Tenta reconectar manualmente
     */
    public void reconnect() {
        if (connecting.get() || connected.get()) {
            if (plugin.config.get().getLogWebsocketInfoInConsole())
                LOGGER.atInfo().log("[WebSocket] Already connected or connecting");
            return;
        }

        if (plugin.config.get().getLogWebsocketInfoInConsole())
            LOGGER.atInfo().log("[WebSocket] Manual reconnection attempt");
        reconnectAttempts.set(0);
        autoReconnect = true;

        if (socket != null) {
            socket.disconnect();
        }

        connect(new HashMap<>());
    }

    /**
     * Emite um evento para o servidor
     *
     * @param event Nome do evento
     * @param args  Argumentos do evento
     */
    public void emit(String event, Object... args) {
        if (socket != null && socket.connected()) {
            socket.emit(event, args);
        } else {
            if (plugin.config.get().getLogWebsocketInfoInConsole())
                LOGGER.atWarning().log("[WebSocket] Failed to emit: " + event);
        }
    }

    /**
     * Registra um listener para um evento
     *
     * @param event    Nome do evento
     * @param listener Função callback
     */
    public void on(String event, Consumer<Object[]> listener) {
        eventListeners.put(event, listener);

        if (socket != null) {
            socket.on(event, args -> {
                listener.accept(args);
            });
        }
    }

    /**
     * Remove um listener de evento
     *
     * @param event Nome do evento
     */
    public void off(String event) {
        eventListeners.remove(event);

        if (socket != null) {
            socket.off(event);
        }
    }

    /**
     * Configura os handlers internos do socket
     */
    private void setupEventHandlers() {
        socket.on(Socket.EVENT_CONNECT, args -> handleConnect());
        socket.on(Socket.EVENT_DISCONNECT, args -> handleDisconnect(args));
        socket.on(Socket.EVENT_CONNECT_ERROR, args -> handleConnectError(args));
        //socket.on(Socket.EVENT_RECONNECT_ATTEMPT, args -> handleReconnectAttempt(args));
        //socket.on(Socket.EVENT_RECONNECT_FAILED, args -> handleReconnectFailed());

        // Handler de heartbeat
        socket.on("heartbeat", args -> {
            if (args.length > 0) {
                String key = args[0].toString();
                String response = key + "." + socket.id();
                socket.emit("heartbeat", response);
            }
        });
    }

    private void handleConnect() {
        if (plugin.config.get().getLogWebsocketInfoInConsole())
            LOGGER.atInfo().log("[WebSocket] Connected successfully");

        connected.set(true);
        connecting.set(false);
        reconnectAttempts.set(0);
        autoReconnect = true;
    }

    private void handleDisconnect(Object[] args) {
        String reason = args.length > 0 ? args[0].toString() : "unknown";
        if (plugin.config.get().getLogWebsocketInfoInConsole())
            LOGGER.atInfo().log("[WebSocket] Disconnected: " + reason);

        connected.set(false);

        if ("io client disconnect".equals(reason) || "io server disconnect".equals(reason)) {
            disconnectForced.set(true);
            connecting.set(false);
            return;
        }

        if (!autoReconnect || disconnectForced.get()) {
            connecting.set(false);
            return;
        }

        // Inicia reconexão automática
        connecting.set(true);
        attemptReconnection();
    }

    private void handleConnectError(Object[] args) {
        String error = args.length > 0 ? args[0].toString() : "Unknown error";
        LOGGER.atSevere().log("[WebSocket] Connection error: " + error);

        connected.set(false);
        connecting.set(false);
    }

    private void handleReconnectAttempt(Object[] args) {
        int attempt = args.length > 0 ? ((Number) args[0]).intValue() : 0;
        if (plugin.config.get().getLogWebsocketInfoInConsole())
            LOGGER.atInfo().log("[WebSocket] Reconnection attempt #" + attempt);
    }

    private void handleReconnectFailed() {
        LOGGER.atWarning().log("[WebSocket] All reconnection attempts failed");

        connected.set(false);
        connecting.set(false);
    }

    /**
     * Lógica de reconexão automática
     */
    private void attemptReconnection() {
        if (!autoReconnect || disconnectForced.get()) {
            return;
        }

        reconnectAttempts.incrementAndGet();

        if (reconnectAttempts.get() > MAX_RECONNECT_ATTEMPTS) {
            LOGGER.atSevere().log("[WebSocket] Connect failed after " + MAX_RECONNECT_ATTEMPTS + " attempts");
            connecting.set(false);
            return;
        }

        if (plugin.config.get().getLogWebsocketInfoInConsole())
            LOGGER.atInfo().log("[WebSocket] Reconnection attempt " + reconnectAttempts.get() + "/" + MAX_RECONNECT_ATTEMPTS);

        scheduler.schedule(() -> {
            if (!connected.get() && !disconnectForced.get()) {
                socket.connect();
                attemptReconnection();
            }
        }, RECONNECT_DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * Cria a query string a partir do mapa
     */
    private String createQueryString(Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        query.forEach((key, value) -> {
            if (sb.length() > 0) sb.append("&");
            sb.append(key).append("=").append(value);
        });

        return sb.toString();
    }

    /**
     * Verifica se está conectado
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Verifica se está conectando
     */
    public boolean isConnecting() {
        return connecting.get();
    }

    /**
     * Verifica se foi desconectado forçadamente
     */
    public boolean isDisconnectForced() {
        return disconnectForced.get();
    }

    /**
     * Obtém o ID do socket
     */
    public String getSocketId() {
        return socket != null ? socket.id() : null;
    }

    /**
     * Shutdown do manager (chamado quando o plugin for desabilitado)
     */
    public void shutdown() {
        LOGGER.atInfo().log("[WebSocket] Shutting down WebSocketManager");

        autoReconnect = false;

        if (socket != null) {
            socket.disconnect();
            socket.close();
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}