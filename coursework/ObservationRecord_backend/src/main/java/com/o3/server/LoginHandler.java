package com.o3.server;

import java.io.IOException;
import com.sun.net.httpserver.*;
import com.sun.net.httpserver.Authenticator.Result;

public class LoginHandler implements HttpHandler {
    UserAuthenticator userAuthenticator;

    public LoginHandler(UserAuthenticator userAuthenticator) {
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
            t.sendResponseHeaders(200, -1); // OK
        } else {
            System.out.println("Authentication failed");
            t.sendResponseHeaders(401, -1); // Unauthorized
            return;
        }
    }

        
    }
    

