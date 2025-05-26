package com.o3.server;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import com.sun.net.httpserver.BasicAuthenticator;


public class UserAuthenticator extends BasicAuthenticator {
    //The comment are old method use before using database if you want to use it without databse you can uncomment it
    //private Map<String,String> users = null;

    public UserAuthenticator() {
        super("datarecord");
        //users = new Hashtable<String,String>();
        //users.put("dummy", "passwd"); 
    }
    
    @Override
    public Result authenticate(HttpExchange exchange) {
        // Extract Authorization header
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return new Failure(401); // Unauthorized if no valid auth header
        }

        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        final String[] values = credentials.split(":", 2);
        final String username = values[0];
        final String password = values[1];

        // Check the credentials
        if (!checkCredentials(username, password)) {
            return new Failure(401); // Unauthorized if credentials are wrong
        }

        // Successfully authenticated, set principal
        HttpPrincipal principal = new HttpPrincipal(username, "yourRealm");
        return new Success(principal);
    }

    @Override
    public boolean checkCredentials(String user, String passwd) {
        //String storedPasswd = users.get(user);
        //return storedPasswd != null && storedPasswd.equals(passwd);
        try{
        return MessageDatabase.getInstance().authenticateUser(user, passwd);
        }catch(Exception e){
            return false;
        }
    }

    /*public boolean addUser(User user) {
        if (users.containsKey(user.getUsername())) {
            return false;
        }else {
            users.put(user.getUsername(), user.getPassword());
            return true;
        }
       }
*/
}
