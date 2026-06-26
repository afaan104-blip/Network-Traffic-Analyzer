package org.example;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ApiServer {

    public static void start() throws IOException {

        HttpServer server =
                HttpServer.create(new InetSocketAddress(8080), 0);

        // START CAPTURE
        server.createContext("/api/start", ex -> {
            TsharkEngine.start();
            send(ex, "{\"status\":\"started\"}");
        });

        // STOP CAPTURE
        server.createContext("/api/stop", ex -> {
            TsharkEngine.stop();
            send(ex, "{\"status\":\"stopped\"}");
        });
        // RESET
        server.createContext("/api/reset", ex -> {
            TsharkEngine.reset();
            send(ex, "{\"status\":\"reset\"}");
        });

        // STATUS (capture running + packet count, for restoring UI when returning to Dashboard)
        server.createContext("/api/status", ex -> {
            boolean running = TsharkEngine.isRunning();
            int packetCount = TsharkEngine.packets.size();
            String json = "{\"running\":" + running + ",\"packetCount\":" + packetCount + "}";
            send(ex, json);
        });

        // LIVE PACKETS (used by frontend)
        server.createContext("/api/packet", ex -> {
            send(ex, TsharkEngine.packetsToJson());
        });

        // ALERTS (used by frontend)
        server.createContext("/api/alerts", ex -> {
            send(ex, TsharkEngine.alertsToJson());
        });

        server.start();

        System.out.println("API running at http://localhost:8080");
    }

    private static void send(
            com.sun.net.httpserver.HttpExchange ex,
            String body) throws IOException {

        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Content-Type", "application/json");

        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, b.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }
}
