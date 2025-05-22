package com.hind.spring.authentication.dto;

import com.hind.spring.authentication.model.ERole;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class RegisterUserDTO {
    private String username;
    private String email;
    private String password;
    private String phone;
    private String fullName;
    private String role;;
}
