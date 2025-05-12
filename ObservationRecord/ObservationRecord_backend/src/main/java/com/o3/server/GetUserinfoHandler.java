package com.o3.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


import com.sun.net.httpserver.*;
import com.sun.net.httpserver.Authenticator.Result;
import org.json.JSONObject;

public class GetUserinfoHandler implements HttpHandler {
    UserAuthenticator userAuthenticator;
    HttpPrincipal principal;

    public GetUserinfoHandler(UserAuthenticator userAuthenticator) {
        this.userAuthenticator = userAuthenticator;
    }   

    @Override
    public void handle(HttpExchange t) throws IOException {
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        t.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

        if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
            // Preflight request, respond with OK and exit
            System.out.println("Preflight request received");
            t.sendResponseHeaders(204, -1); // No Content
            return;
        }

        Result result = userAuthenticator.authenticate(t);

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

    //Send userNcikname when GET
    private void handleGet(HttpExchange exchange) throws IOException {
        try{
        JSONObject userNicknameJSON = new JSONObject();
        String userNickname = (principal != null) ? MessageDatabase.getInstance().getUserNickname(principal.getUsername()) : "Unknown";
        userNicknameJSON.put("userNickname", userNickname);

        // Send the response
        sendResponse(exchange, 200, userNicknameJSON.toString());
        } catch (Exception e) {
            System.out.println("Error: " + e);
            exchange.sendResponseHeaders(400, -1);
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