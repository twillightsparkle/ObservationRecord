package com.o3.server;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.sun.net.httpserver.*;
import com.sun.net.httpserver.Authenticator.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class RatingHandler implements HttpHandler {
    UserAuthenticator userAuthenticator;
    HttpPrincipal principal;

    public RatingHandler(UserAuthenticator userAuthenticator) {
        this.userAuthenticator = userAuthenticator;
    }   

    @Override
    public void handle(HttpExchange t) throws IOException {
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

        if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
            t.sendResponseHeaders(204, -1);
            return;
        }

        Result result = userAuthenticator.authenticate(t);

        if (!(result instanceof Authenticator.Success)) {
            System.out.println("Authentication failed");
            t.sendResponseHeaders(401, -1);
            return;
        }

        principal = ((Authenticator.Success) result).getPrincipal();

        try {
            if ("GET".equalsIgnoreCase(t.getRequestMethod())) {
                handleGet(t);
            } else if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                handlePost(t);
            } else {
                t.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        } catch (Exception e) {
            e.printStackTrace();
            t.sendResponseHeaders(500, -1); // Internal Server Error
        }
    }

    private void handleGet(HttpExchange t) throws IOException, SQLException {
        //Nothing yet
    }

    private void handlePost(HttpExchange t) throws IOException, SQLException {
        String username = principal.getUsername();
        BufferedReader reader = new BufferedReader(new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8));
        StringBuilder requestBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            requestBody.append(line);
        }

        if (requestBody.toString().trim().isEmpty()) {
            t.sendResponseHeaders(400, -1);
            return;
        }

        JSONObject json = new JSONObject(requestBody.toString());
        if (!json.has("id") || !json.has("rating")) {
            t.sendResponseHeaders(400, -1);
            return;
        }

        int recordId = json.getInt("id");
        int rating = json.getInt("rating");

        boolean success = MessageDatabase.getInstance().saveRating(recordId, username, rating);
        t.sendResponseHeaders(success ? 200 : 500, -1);
    }

    // Parse query parameters
    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }
    
}
