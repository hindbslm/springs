package com.hind.spring.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUserDTO {
    private String email;
    private String password;
    private String verificationCode;

    @Override
    public String toString() {
        return "PasswordUserDTO{" +
                "email='" + email + '\'' +
                ", password='" + (password != null ? "[PROVIDED]" : null) + '\'' +
                ", verificationCode='" + verificationCode + '\'' +
                '}';
    }
}