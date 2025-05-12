package com.user_service.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminListGroupsForUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminListGroupsForUserResult;
import com.amazonaws.services.cognitoidp.model.GroupType;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AWSCognitoIdentityProvider cognitoClient;

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    public UserService(UserRepository userRepository, AWSCognitoIdentityProvider cognitoClient) {
        this.userRepository = userRepository;
        this.cognitoClient = cognitoClient;
    }

    /**
     * Registers a new user in the local database, syncing data from Cognito.
     * @param user the user to register
     * @param cognitoSub the Cognito user sub (unique identifier)
     * @return the saved user
     * @throws IllegalArgumentException if email already exists
     */
    public User registerUser(@Valid User user, String cognitoSub) {
        if (userRepository.existsByEmail(user.getEmail())) {
            logger.warn("Registration failed: Email already exists: {}", user.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }
        user.setCognitoSub(cognitoSub);
        user.setRole(Role.CUSTOMER); // Default role
        syncRolesFromCognito(user);
        logger.info("Registering user with email: {}", user.getEmail());
        return userRepository.save(user);
    }

    /**
     * Finds a user by email, ignoring case.
     * @param email the email to search for
     * @return the user
     * @throws IllegalArgumentException if user not found
     */
    public User findByEmail(String email) {
        logger.debug("Searching for user with email: {}", email);
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    logger.warn("User not found for email: {}", email);
                    return new IllegalArgumentException("User not found");
                });
    }

    /**
     * Syncs Cognito groups to local user roles.
     * @param user the user to update
     */
    private void syncRolesFromCognito(User user) {
        try {
            AdminListGroupsForUserRequest request = new AdminListGroupsForUserRequest()
                    .withUserPoolId(userPoolId)
                    .withUsername(user.getEmail());

            AdminListGroupsForUserResult result = cognitoClient.adminListGroupsForUser(request);
            for (GroupType group : result.getGroups()) {
                String groupName = group.getGroupName();
                try {
                    Role role = Role.valueOf(groupName.toUpperCase());
                    user.setRole(role);
                    logger.debug("Assigned role {} to user {}", role, user.getEmail());
                } catch (IllegalArgumentException e) {
                    logger.warn("Unknown Cognito group {} for user {}", groupName, user.getEmail());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to sync Cognito groups for user {}: {}", user.getEmail(), e.getMessage());
        }
    }
}