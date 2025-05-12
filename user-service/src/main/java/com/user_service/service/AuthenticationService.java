//package com.user_service.service;
//
//import com.user_service.entity.User;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.stereotype.Service;
//
//@Service
//public class AuthenticationService {
//
//    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
//
//    private final UserService userService;
//    private final JwtService jwtService;
//    private final AuthenticationManager authenticationManager;
//
//    public AuthenticationService(UserService userService, JwtService jwtService, AuthenticationManager authenticationManager) {
//        this.userService = userService;
//        this.jwtService = jwtService;
//        this.authenticationManager = authenticationManager;
//    }
//
//    public String authenticate(String email, String password) throws AuthenticationException {
//        logger.debug("Attempting authentication for email: {}", email);
//
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(email, password)
//        );
//
//        try {
//            User user = userService.findByEmail(email);
//            logger.info("Authentication successful for email: {}", email);
//            return jwtService.generateToken(email, user.getRole().name());
//        } catch (IllegalArgumentException e) {
//            logger.error("User not found after authentication: {}", email);
//            throw new IllegalStateException("User not found after authentication: " + email, e);
//        }
//    }
//}
package com.user_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(UserService userService, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public String authenticate(String email, String password) throws AuthenticationException {
        logger.debug("Attempting authentication for email: {}", email);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        // Extract role from UserDetails
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElseThrow(() -> {
                    logger.error("No role found for user: {}", email);
                    return new IllegalStateException("No role found for user: " + email);
                });

        logger.info("Authentication successful for email: {}", email);
        return jwtService.generateToken(email, role);
    }
}