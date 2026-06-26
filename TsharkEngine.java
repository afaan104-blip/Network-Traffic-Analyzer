package org.example;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.example.GeoIPService;

public class TsharkEngine {

    private static Process process;
    private static Thread reader;
    private static volatile boolean running = false;

    public static List<Map<String, String>> packets =
            new CopyOnWriteArrayList<>();

    public static List<Map<String, String>> alerts =
            new CopyOnWriteArrayList<>();


    // START CAPTURE
    public static synchronized void start() throws IOException {

        if (running) return;
        running = true;

        ProcessBuilder pb = new ProcessBuilder(
                "tshark",
                "-i", "7",
                "-i", "4",
                "-l",
                "-T", "fields",
                "-E", "separator=|",
                "-e", "frame.time_epoch",
                "-e", "ip.src",
                "-e", "ip.dst",
                "-e", "ip.proto",
                "-e", "frame.len"
        );

        pb.redirectErrorStream(true);
        process = pb.start();

        reader = new Thread(() -> {
            try (BufferedReader br =
                         new BufferedReader(
                                 new InputStreamReader(process.getInputStream()))) {

                String line;

                while (running && (line = br.readLine()) != null) {
                    parse(line);
                }

            } catch (Exception ignored) {
            }
        });

        reader.start();
    }

    /**
     * Whether capture is currently running (for frontend restore on tab return).
     */
    public static boolean isRunning() {
        return running;
    }

    // STOP CAPTURE
    public static synchronized void stop() {
        running = false;

        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    // reset
    public static synchronized void reset() {
        packets.clear();
        alerts.clear();
    }

    // PARSE TSHARK OUTPUT
    private static void parse(String line) {

        String[] p = line.split("\\|");
        if (p.length < 5) return;

        Map<String, String> pkt = new HashMap<>();

        pkt.put("time", p[0]);
        pkt.put("src", p[1]);
        pkt.put("dst", p[2]);
        pkt.put("protocol", protocolName(p[3]));
        pkt.put("size", String.valueOf(Integer.parseInt(p[4])));
        double[] loc = GeoIPService.lookup(p[1]);
        if (loc != null) {
            pkt.put("lat", String.valueOf(loc[0]));
            pkt.put("lon", String.valueOf(loc[1]));
            pkt.put("country", GeoIPService.getCountry(p[1]));
        }
        packets.add(pkt);

        // ALERT LOGIC
        // ALERT LOGIC
        try {
            int size = Integer.parseInt(p[4]);

            if (size > 12000) {
                addAlert("CRITICAL",
                        "Abnormally large packet detected",
                        pkt);
            } else if (size > 9000) {
                addAlert("WARNING",
                        "Jumbo packet detected",
                        pkt);
            }

        } catch (Exception ignored) {
        }

        if (packets.size() > 1111000)
            packets.remove(0);

        if (alerts.size() > 11100)
            alerts.remove(0);
    }

    // ADD STRUCTURED ALERT
    private static void addAlert(String type, String message, Map<String, String> pkt) {

        // Remove duplicates (same src + same type)
        for (Map<String, String> a : alerts) {
            if (a.get("src").equals(pkt.get("src")) &&
                    a.get("type").equals(type)) {
                return; // duplicate detected
            }
        }

        Map<String, String> alert = new HashMap<>();
        alert.put("time", pkt.get("time"));
        alert.put("src", pkt.get("src"));
        alert.put("dst", pkt.get("dst"));
        alert.put("protocol", pkt.get("protocol"));
        alert.put("size", pkt.get("size"));
        alert.put("type", type);
        alert.put("message", message);

        alerts.add(alert);
    }

    private static String protocolName(String id) {
        if (id.equals("6")) return "TCP";
        if (id.equals("17")) return "UDP";
        if (id.equals("1")) return "ICMP";
        return "OTHER";
    }

    // JSON PACKETS
    public static String packetsToJson() {

        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < packets.size(); i++) {

            Map<String, String> m = packets.get(i);

            sb.append("{")
                    .append("\"time\":\"").append(m.get("time")).append("\",")
                    .append("\"src\":\"").append(m.get("src")).append("\",")
                    .append("\"dst\":\"").append(m.get("dst")).append("\",")
                    .append("\"protocol\":\"").append(m.get("protocol")).append("\",")
                    .append("\"size\":\"").append(m.get("size")).append("\",")
                    .append("\"lat\":\"").append(m.getOrDefault("lat", "")).append("\",")
                    .append("\"lon\":\"").append(m.getOrDefault("lon", "")).append("\",")
                    .append("\"country\":\"").append(m.getOrDefault("country", "Unknown")).append("\"")
                    .append("}");

            if (i < packets.size() - 1)
                sb.append(",");
        }

        sb.append("]");
        return sb.toString();
    }

    // JSON ALERTS
    public static String alertsToJson() {

        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < alerts.size(); i++) {

            Map<String, String> a = alerts.get(i);

            sb.append("{")
                    .append("\"time\":\"").append(a.get("time")).append("\",")
                    .append("\"src\":\"").append(a.get("src")).append("\",")
                    .append("\"dst\":\"").append(a.get("dst")).append("\",")
                    .append("\"protocol\":\"").append(a.get("protocol")).append("\",")
                    .append("\"size\":\"").append(a.get("size")).append("\",")
                    .append("\"type\":\"").append(a.get("type")).append("\",")
                    .append("\"message\":\"").append(a.get("message")).append("\"")
                    .append("}");

            if (i < alerts.size() - 1)
                sb.append(",");
        }

        sb.append("]");
        return sb.toString();
    }
}