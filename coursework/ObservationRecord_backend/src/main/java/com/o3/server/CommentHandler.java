package com.o3.server;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.*;
import com.sun.net.httpserver.Authenticator.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class CommentHandler implements HttpHandler{
    UserAuthenticator userAuthenticator;
    HttpPrincipal principal;

    public CommentHandler(UserAuthenticator userAuthenticator) {
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
                // Handle GET request if needed
                handleGet(t);
            } else
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
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
        String query = t.getRequestURI().getQuery();
        if (query == null || query.trim().isEmpty()) {
            t.sendResponseHeaders(400, -1);
            return;
        }

        // Parse query parameters
        Map<String, String> params = parseQuery(query);
        if (params.size() != 1 || !params.containsKey("id")) {
            t.sendResponseHeaders(400, -1);
            return;
        }

        int recordId = Integer.parseInt(params.get("id"));
        StringBuilder responseBuilder = new StringBuilder();
        try{
            JSONArray responseMessages = MessageDatabase.getInstance().getComments(recordId);
            if (responseMessages.length() == 0) {
                responseBuilder.append("No Comments");
            } else {
                responseBuilder.append(responseMessages.toString(4));
         }
            String response = responseBuilder.toString().trim(); // Remove trailing newline
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

            t.sendResponseHeaders(200, responseBytes.length);
        
            OutputStream outputStream = t.getResponseBody();
            outputStream.write(responseBytes);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            System.out.println("Error: " + e);
            t.sendResponseHeaders(400, -1);
        }
    }

    private void handlePost(HttpExchange t) throws IOException, SQLException {
        String username = principal.getUsername();
        String userNickname = MessageDatabase.getInstance().getUserNickname(principal.getUsername());
        InputStreamReader isr = new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(isr);
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }

        JSONObject json = new JSONObject(body.toString());
        if (!json.has("id") || !json.has("comment")) {
            t.sendResponseHeaders(400, -1);
            return;
        }

        int recordId = json.getInt("id");
        String commentText = json.getString("comment");
        boolean success = MessageDatabase.getInstance().saveComment(recordId, username, userNickname, commentText);
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
