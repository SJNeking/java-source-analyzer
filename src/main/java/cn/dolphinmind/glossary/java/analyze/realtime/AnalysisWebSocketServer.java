package cn.dolphinmind.glossary.java.analyze.realtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket server that broadcasts analysis progress events to connected clients.
 */
public class AnalysisWebSocketServer {
    private static final int DEFAULT_PORT = 8887;

    private final int port;
    private WebSocketServer server;
    private final CopyOnWriteArrayList<WebSocket> connections = new CopyOnWriteArrayList<>();
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public AnalysisWebSocketServer(int port) {
        this.port = port;
    }

    public AnalysisWebSocketServer() {
        this(DEFAULT_PORT);
    }

    /**
     * Start the WebSocket server (non-blocking).
     */
    public void start() {
        server = new WebSocketServer(new InetSocketAddress(port)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                connections.add(conn);
                System.out.println("🔌 WebSocket 客户端已连接: " + conn.getRemoteSocketAddress());

                // Send welcome message
                Map<String, Object> welcome = new LinkedHashMap<>();
                welcome.put("type", "CONNECTION_ESTABLISHED");
                welcome.put("message", "已连接到 Java Source Analyzer 实时分析流");
                welcome.put("progress", 0);
                conn.send(gson.toJson(welcome));
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                connections.remove(conn);
                System.out.println("🔌 WebSocket 客户端已断开: " + conn.getRemoteSocketAddress());
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                // Ignore incoming messages for now (broadcast only)
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                System.err.println("❌ WebSocket 错误: " + ex.getMessage());
                if (conn != null) {
                    connections.remove(conn);
                }
            }

            @Override
            public void onStart() {
                System.out.println("🚀 WebSocket 服务器已启动: ws://localhost:" + port);
            }
        };

        server.setConnectionLostTimeout(60); // 60 second timeout
        server.start();
    }

    /**
     * Broadcast a progress event to all connected clients.
     */
    public void broadcast(AnalysisProgressEvent event) {
        String json = gson.toJson(event.toMap());
        broadcast(json);
    }

    /**
     * Broadcast a raw JSON message to all connected clients.
     */
    public void broadcast(String json) {
        for (WebSocket conn : new CopyOnWriteArrayList<>(connections)) {
            try {
                if (conn.isOpen()) {
                    conn.send(json);
                }
            } catch (Exception e) {
                // Connection may have closed
                connections.remove(conn);
            }
        }
    }

    /**
     * Stop the WebSocket server.
     */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.stop();
            System.out.println("🛑 WebSocket 服务器已停止");
        }
    }

    /**
     * Get the number of connected clients.
     */
    public int getClientCount() {
        return connections.size();
    }

    /**
     * Get the server port.
     */
    public int getPort() {
        return port;
    }
}
