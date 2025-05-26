package com.o3.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.sun.net.httpserver.Authenticator.Result;


import org.json.JSONArray;

import com.sun.net.httpserver.*;

public class SearchHandler implements HttpHandler {
    UserAuthenticator userAuthenticator;
    HttpPrincipal principal;

    public SearchHandler(UserAuthenticator userAuthenticator) {
        this.userAuthenticator = userAuthenticator;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

        if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
            // Preflight request, respond with OK and exit
            t.sendResponseHeaders(204, -1); // No Content
            return;
        }

        Result result = userAuthenticator.authenticate(t);
        if (!(result instanceof Authenticator.Success)) {
            System.out.println("Authentication failed");
            t.sendResponseHeaders(401, -1); // Unauthorized
            return;
        }
        if (result instanceof Authenticator.Success) {
            principal = ((Authenticator.Success) result).getPrincipal();
        } else {
            System.out.println("Authentication failed");
            t.sendResponseHeaders(401, -1); // Unauthorized
            return;
        }
        

        //implement GET and POST handling 
        if (t.getRequestMethod().equalsIgnoreCase("GET")) {
            // Handle POST requests here (users send this for sending messages)
            handleGet(t);
        } else {
            //400 with a message “Not supported” (witouth the “).
            sendResponse(t, 400, "Not supported");
        }
        
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        try{
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.trim().isEmpty()) {
            sendResponse(exchange, 400, "Query cannot be empty");
            return;
        }

        // Parse query parameters
        Map<String, String> params = parseQuery(query);

        String nickname = params.get("nickname") != null 
            ? URLDecoder.decode(params.get("nickname"), StandardCharsets.UTF_8) 
            : null;
        String observationId = params.get("identification") != null 
            ? URLDecoder.decode(params.get("identification"), StandardCharsets.UTF_8) 
            : null;
        Long fromDateStr = params.containsKey("after") ? Instant.parse(params.get("after")).toEpochMilli() : null;
        Long toDateStr = params.containsKey("before") ? Instant.parse(params.get("before")).toEpochMilli() : null;

        JSONArray result = MessageDatabase.getInstance().search(nickname, observationId, fromDateStr, toDateStr, principal.getUsername());

        sendResponse(exchange, 200, result.toString());
        }
        catch(Exception e){
            sendResponse(exchange, 400, "Invalid query parameters");
        }
    }

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


    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
    }
    
}
