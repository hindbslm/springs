package com.hind.spring.authentication.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeUser {

    private String email;
    private String oldPassword;
    private String password;
}
