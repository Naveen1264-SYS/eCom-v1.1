package com.user_service.controller;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.*;
import com.user_service.entity.User;
import com.user_service.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AWSCognitoIdentityProvider cognitoClient;
    private final UserService userService;

    @Value("${aws.cognito.clientId}")
    private String clientId;

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    public AuthController(AWSCognitoIdentityProvider cognitoClient, UserService userService) {
        this.cognitoClient = cognitoClient;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            SignUpRequest signUpRequest = new SignUpRequest()
                    .withClientId(clientId)
                    .withUsername(registerRequest.getEmail())
                    .withPassword(registerRequest.getPassword())
                    .withUserAttributes(
                            new AttributeType().withName("email").withValue(registerRequest.getEmail())
                    );

            SignUpResult result = cognitoClient.signUp(signUpRequest);

            // Assign user to CUSTOMER group
            AdminAddUserToGroupRequest groupRequest = new AdminAddUserToGroupRequest()
                    .withUserPoolId(userPoolId)
                    .withUsername(registerRequest.getEmail())
                    .withGroupName("CUSTOMER");
            cognitoClient.adminAddUserToGroup(groupRequest);

            // Sync user to local database
            User user = new User();
            user.setEmail(registerRequest.getEmail());
            userService.registerUser(user, result.getUserSub());

            logger.info("User registered successfully: {}", registerRequest.getEmail());
            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully",
                    "userSub", result.getUserSub()
            ));
        } catch (UsernameExistsException e) {
            logger.warn("Registration failed: Email already exists: {}", registerRequest.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Email already exists"));
        } catch (AWSCognitoIdentityProviderException e) {
            logger.warn("Registration failed: {}", e.getErrorMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getErrorMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during registration for email {}: {}", registerRequest.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed due to server error"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", loginRequest.getEmail());
            authParams.put("PASSWORD", loginRequest.getPassword());

            InitiateAuthRequest authRequest = new InitiateAuthRequest()
                    .withAuthFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .withClientId(clientId)
                    .withAuthParameters(authParams);

            InitiateAuthResult result = cognitoClient.initiateAuth(authRequest);
            String idToken = result.getAuthenticationResult().getIdToken();
            logger.info("User logged in successfully: {}", loginRequest.getEmail());
            return ResponseEntity.ok(Map.of("token", idToken));
        } catch (NotAuthorizedException e) {
            logger.warn("Authentication failed for email {}: Invalid credentials", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        } catch (UserNotConfirmedException e) {
            logger.warn("Authentication failed for email {}: User not confirmed", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User email not verified"));
        } catch (AWSCognitoIdentityProviderException e) {
            logger.warn("Authentication failed for email {}: {}", loginRequest.getEmail(), e.getErrorMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed: " + e.getErrorMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during authentication for email {}: {}", loginRequest.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed due to server error"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                logger.warn("Invalid or missing Authorization header");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid or missing Authorization header"));
            }
            String token = authorizationHeader.substring(7);
            GlobalSignOutRequest signOutRequest = new GlobalSignOutRequest()
                    .withAccessToken(token);
            cognitoClient.globalSignOut(signOutRequest);
            logger.info("Logout successful for token ending in {}", token.substring(token.length() - 4));
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (NotAuthorizedException e) {
            logger.warn("Logout failed: Invalid token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid token"));
        } catch (AWSCognitoIdentityProviderException e) {
            logger.warn("Logout failed: {}", e.getErrorMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getErrorMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Logout failed due to server error"));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                logger.warn("Invalid or missing Authorization header");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid or missing Authorization header"));
            }
            String accessToken = authorizationHeader.substring(7);
            com.amazonaws.services.cognitoidp.model.ChangePasswordRequest cognitoRequest = new ChangePasswordRequest()
                    .withAccessToken(accessToken)
                    .withPreviousPassword(changePasswordRequest.getCurrentPassword())
                    .withProposedPassword(changePasswordRequest.getNewPassword());
            cognitoClient.changePassword(cognitoRequest);
            logger.info("Password changed successfully for token ending in {}", accessToken.substring(accessToken.length() - 4));
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (NotAuthorizedException e) {
            logger.warn("Password change failed: Invalid token or credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token or current password"));
        } catch (AWSCognitoIdentityProviderException e) {
            logger.warn("Password change failed: {}", e.getErrorMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getErrorMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during password change: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Password change failed due to server error"));
        }
    }

    static class RegisterRequest {
        @NotBlank(message = "Email is required")
        private String email;
        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    static class LoginRequest {
        @NotBlank(message = "Email is required")
        private String email;
        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;
        @NotBlank(message = "New password is required")
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public com.amazonaws.services.cognitoidp.model.ChangePasswordRequest withAccessToken(String accessToken) {
            return null;
        }
    }
}