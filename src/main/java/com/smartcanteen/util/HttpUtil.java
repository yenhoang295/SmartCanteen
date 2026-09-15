package com.smartcanteen.util;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpUtil {

    public static JSONObject readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int n;
        while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
        String body = buf.toString(StandardCharsets.UTF_8);
        if (body.isBlank()) return new JSONObject();
        return new JSONObject(body);
    }

    public static Map<String, String> queryParams(HttpExchange ex) {
        Map<String, String> result = new HashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) return result;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            result.put(key, val);
        }
        return result;
    }

    public static void sendJson(HttpExchange ex, int statusCode, Object payload) throws IOException {
        String json = (payload instanceof String) ? (String) payload : payload.toString();
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange ex, int statusCode, String message) throws IOException {
        JSONObject err = new JSONObject().put("error", message);
        sendJson(ex, statusCode, err.toString());
    }
}
