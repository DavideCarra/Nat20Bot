package core;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class WebStub {
    // TODO dummy endpoint to keep bot online, to remove in future deployment
    public static void start() throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")); // Render usa la variabile PORT
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            String response = "Hello from dnd_bot!";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.setExecutor(null);
        server.start();
        System.out.println("HTTP server started on port " + port);
    }
}