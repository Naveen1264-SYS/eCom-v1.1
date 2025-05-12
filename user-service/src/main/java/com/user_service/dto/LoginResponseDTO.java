package com.user_service.dto;

import com.user_service.enums.Role;
import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token;
    private String email;
    private Role role;

    public void LoginResponseDTO(String token, String email, Role role) {
        this.token = token;
        this.email = email;
        this.role = role;
    }
}
