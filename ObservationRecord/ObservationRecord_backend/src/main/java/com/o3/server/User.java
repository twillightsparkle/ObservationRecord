package com.o3.server;

public class User {
    // format { “username” : “username”, “password” : “password”, “email” : “user.email@for-contacting.com”}
    private String username;
    private String password;
    private String email;
    private String userNickname;

    public User(String username, String password, String email, String userNickname) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.userNickname = userNickname;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getUserNickname() {
        return userNickname;
    }
}
