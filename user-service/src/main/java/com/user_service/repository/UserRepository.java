package com.user_service.repository;

import com.user_service.entity.User;
import com.user_service.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Finds a user by their email address.
     * @param email the email to search for
     * @return an Optional containing the user, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their email address, ignoring case.
     * @param email the email to search for
     * @return an Optional containing the user, or empty if not found
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    /**
     * Finds a user by their email and role.
     * @param email the email to search for
     * @param role the role to match
     * @return an Optional containing the user, or empty if not found
     */
    Optional<User> findByEmailAndRole(String email, Role role);

    /**
     * Checks if a user exists by their email.
     * @param email the email to check
     * @return true if a user with the email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds all users with the specified role.
     * @param role the role to search for
     * @return a list of users with the given role
     */
    List<User> findByRole(Role role);

    /**
     * Finds all users with the specified role, with pagination.
     * @param role the role to search for
     * @param pageable pagination information
     * @return a page of users with the given role
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Finds a user by their phone number.
     * @param phoneNumber the phone number to search for
     * @return an Optional containing the user, or empty if not found
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
}