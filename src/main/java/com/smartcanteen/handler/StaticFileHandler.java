package com.smartcanteen.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class StaticFileHandler implements HttpHandler {

    private final File rootDir;

    public StaticFileHandler(String rootPath) {
        this.rootDir = new File(rootPath);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        File file = new File(rootDir, path).getCanonicalFile();
        // Bao ve khong cho truy cap ra ngoai thu muc public
        if (!file.getPath().startsWith(rootDir.getCanonicalPath())) {
            ex.sendResponseHeaders(403, -1);
            return;
        }
        if (!file.exists() || file.isDirectory()) {
            ex.sendResponseHeaders(404, -1);
            return;
        }

        String contentType = guessContentType(file.getName());
        byte[] bytes = Files.readAllBytes(file.toPath());
        ex.getResponseHeaders().add("Content-Type", contentType);
        ex.sendResponseHeaders(200, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String guessContentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
