package com.o3.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.*;
import com.sun.net.httpserver.Authenticator.Result;




public class ViewHandler implements HttpHandler {
    UserAuthenticator userAuthenticator;

    public ViewHandler(UserAuthenticator userAuthenticator) {
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
            try{
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
            MessageDatabase.getInstance().incrementViewCount(recordId);
            } catch (Exception e) {
            e.printStackTrace();
        }
        
            t.sendResponseHeaders(200, -1); // OK
        } else {
            System.out.println("Authentication failed");
            t.sendResponseHeaders(401, -1); // Unauthorized
            return;
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
