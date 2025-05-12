package com.o3.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


import org.json.JSONObject;

import com.sun.net.httpserver.*;

public class RegistrationHandler implements HttpHandler{

    public RegistrationHandler() {
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        t.getResponseHeaders().add("Content-Type", "application/json");

        if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
            // Preflight request, respond with OK and exit
            System.out.println("Preflight request received");
            t.sendResponseHeaders(204, -1); // No Content
            return;
        }
        //implement GET and POST handling 
        if (t.getRequestMethod().equalsIgnoreCase("POST")) {
            // Handle POST requests here (users send this for sending messages)
            handlePost(t);
        } else {
            //400 with a message “Not supported” (witouth the “).
            sendResponse(t, 400, "Not supported");
        }
        
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        // Read request body
        InputStream inputStream = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String requestBody = reader.lines().reduce("", (acc, line) -> acc + line);
        reader.close();
        inputStream.close();

    try{
        JSONObject json = new JSONObject(requestBody);

        if(!json.has("username") || !json.has("password") || !json.has("email")){
            sendResponse(exchange, 400, "Username, password or email cannot be empty");
            return;
        }


        if (json.getString("username").isEmpty() || json.getString("password").isEmpty() || json.getString("email").isEmpty()) {
            sendResponse(exchange, 400, "Username, password or email cannot be empty");
            return;
        }

        User user = new User(json.getString("username"), json.getString("password"), json.getString("email"), json.getString("userNickname"));
        boolean success = MessageDatabase.getInstance().setUser(user);
        if (success) {
            sendResponse(exchange, 200, "User registered successfully");
        } else {
            sendResponse(exchange, 403, "User already registered");
        }
    } catch (Exception e) {
        sendResponse(exchange, 400, "Invalid JSON");
    }
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
