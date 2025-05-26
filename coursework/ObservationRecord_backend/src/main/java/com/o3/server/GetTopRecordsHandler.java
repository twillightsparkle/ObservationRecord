package com.o3.server;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

import com.sun.net.httpserver.*;
import com.sun.net.httpserver.Authenticator.Result;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;


public class GetTopRecordsHandler implements HttpHandler {
    UserAuthenticator userAuthenticator;
    HttpPrincipal principal;

    public GetTopRecordsHandler(UserAuthenticator userAuthenticator) {
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
        if (query == null || query.isEmpty()) {
            t.sendResponseHeaders(400, -1); // Bad Request
            return;
        }

        Map<String, String> params = parseQuery(query);
        String base = params.get("base");

        if (base == null || (!base.equals("views") && !base.equals("ratings") && !base.equals("comments")) && !base.equals("random")) {
            t.sendResponseHeaders(400, -1); // Bad Request
            return;
        }

        StringBuilder responseBuilder = new StringBuilder();
        try{
            //get the top 5 records based on the base parameter
        JSONArray responseMessages = new JSONArray();
        if (base.equals("views")) {
            responseMessages = MessageDatabase.getInstance().getTopMostViewedRecords(5,principal.getUsername());
        } else if (base.equals("ratings")) {
            responseMessages = MessageDatabase.getInstance().getTopHighestRatedRecords(5,principal.getUsername());
        } else if (base.equals("comments")) {
            responseMessages = MessageDatabase.getInstance().getTopMostCommentedRecords(5,principal.getUsername());
        } else if (base.equals("random")) {
            responseMessages = MessageDatabase.getInstance().getRandomRecords(10,principal.getUsername());
        }
        if (responseMessages.length() == 0) {
            responseBuilder.append("No messages");
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
