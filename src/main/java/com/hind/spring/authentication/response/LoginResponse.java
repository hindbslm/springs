package com.hind.spring.authentication.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private long expiresIn;
    private String username;
    private String email;
    private String phone;
    private String fullName;

    public LoginResponse(String token, long expiresIn,  String username, String email, String phone, String fullName) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.fullName = fullName;
    }
}