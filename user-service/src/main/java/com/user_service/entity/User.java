package com.user_service.entity;

import com.user_service.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank
    @Column(length = 50, nullable = false)
    private String firstName;

    @NotBlank
    @Column(length = 50, nullable = false)
    private String lastName;

    @Email
    @NotBlank
    @Column(length = 100, unique = true, nullable = false)
    @Setter(AccessLevel.NONE)
    private String email;

    @NotBlank
    @Column(length = 255, nullable = false)
    private String password; // Store hashed password

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Column(length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20,nullable = false)
    private Role role = Role.CUSTOMER; // Updated default role to CUSTOMER

    @CreationTimestamp
    @Column(updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Override
    public String toString() {
        return "User{id=" + id + ", firstName='" + firstName + "', lastName='" + lastName + "', email='" + email + "', role=" + role + "}";
    }


    public void setEmail(String email) {
        this.email = email;
    }

    public void setCognitoSub(String cognitoSub) {
    }
}