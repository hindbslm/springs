package com.hind.spring.authentication.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import com.hind.spring.authentication.dto.*;
import com.hind.spring.authentication.model.ERole;
import com.hind.spring.authentication.model.Role;
import com.hind.spring.authentication.repository.RoleRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hind.spring.authentication.model.User;
import com.hind.spring.authentication.repository.UserRepository;

import jakarta.mail.MessagingException;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final RoleRepository roleRepository;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            RoleRepository roleRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.roleRepository = roleRepository;
    }

    public User signup(RegisterUserDTO input) {
        System.out.println("DEBUG: Starting signup process");
        System.out.println("DEBUG: Input role: " + input.getRole());

        // Find the role from the database
        Role userRole;
        if (input.getRole() != null && !input.getRole().isEmpty()) {
            try {
                ERole eRole = ERole.valueOf(input.getRole().toUpperCase());
                System.out.println("DEBUG: Parsed ERole: " + eRole);

                userRole = roleRepository.findByName(eRole)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + input.getRole()));
                System.out.println("DEBUG: Found role in DB: " + userRole.getName());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid role: " + input.getRole());
            }
        } else {
            // Default to USER role if none specified
            System.out.println("DEBUG: Using default ROLE_USER");
            userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
        }

        // Create user with the role
        User user = new User();
        user.setUsername(input.getUsername());
        user.setEmail(input.getEmail());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setPhone(input.getPhone());
        user.setFullName(input.getFullName());
        user.setRole(userRole); // Make sure this is set!

        System.out.println("DEBUG: User role before save: " + (user.getRole() != null ? user.getRole().getName() : "NULL"));

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);

        sendVerificationEmail(user);

        try {
            User savedUser = userRepository.save(user);
            System.out.println("DEBUG: User saved successfully with ID: " + savedUser.getId());
            return savedUser;
        } catch (Exception e) {
            System.out.println("DEBUG: Error saving user: " + e.getMessage());
            throw e;
        }
    }

    public User authenticate(LoginUserDTO input) {
        if (input.getEmail() == null || input.getEmail().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        if (input.getPassword() == null || input.getPassword().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Please verify your account.");
        }

        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            input.getEmail(),
                            input.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid email or password");
        }

        return user;
    }

    public void verifyUser(VerifyUserDTO input) {
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired");
            }
            if (user.getVerificationCode().equals(input.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationExpiresAt(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Invalid verification code");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account is already verified");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationExpiresAt(LocalDateTime.now().plusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    private void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = "VERIFICATION CODE " + user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    public void forgotPassword(PasswordUserDTO input, String email) {
        // Log the input for debugging
        System.out.println("forgotPassword service method called with email: " + email);
        System.out.println("Input object has email: " + input.getEmail());
        System.out.println("Input object has verification code: " + input.getVerificationCode());
        System.out.println("Input object has password: " + (input.getPassword() != null ? "Yes" : "No"));

        // Double check we're using the correct email - from the input parameter, not the method parameter
        email = (email != null && !email.isEmpty()) ? email : input.getEmail();

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        if (input.getVerificationCode() == null || input.getVerificationCode().isEmpty()) {
            throw new RuntimeException("Verification code is required");
        }

        if (input.getPassword() == null || input.getPassword().isEmpty()) {
            throw new RuntimeException("New password is required");
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // Verify that user has a verification code
            if (user.getVerificationCode() == null) {
                throw new RuntimeException("Please request a verification code first");
            }

            // Check verification code expiration
            if (user.getVerificationExpiresAt() != null &&
                    user.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired. Please request a new one");
            }

            if (user.getVerificationCode().equals(input.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationExpiresAt(null);
                user.setPassword(passwordEncoder.encode(input.getPassword()));
                userRepository.save(user);
                System.out.println("Password reset successful for user: " + email);
            } else {
                System.out.println("Invalid verification code. User code: " + user.getVerificationCode() + ", Input code: " + input.getVerificationCode());
                throw new RuntimeException("Invalid verification code");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void changePassword(PasswordChangeUser input) {
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // Check if the old password matches
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                input.getEmail(),
                                input.getOldPassword()
                        )
                );

                // If we get here, authentication succeeded
                user.setPassword(passwordEncoder.encode(input.getPassword()));
                userRepository.save(user);
            } catch (Exception e) {
                throw new RuntimeException("Current password is incorrect");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void resendVerificationCodeForPassword(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationExpiresAt(LocalDateTime.now().plusHours(1));

            // Send a different email for password reset
            sendPasswordResetEmail(user);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    private void sendPasswordResetEmail(User user) {
        String subject = "Password Reset Request";
        String verificationCode = "PASSWORD RESET CODE " + user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Password Reset Request</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to reset your password:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "<p style=\"font-size: 14px; color: #666; margin-top: 20px;\">If you did not request a password reset, please ignore this email.</p>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }
}