package com.perfect.IndiExport.controller;

import com.perfect.IndiExport.dto.LoginRequest;
import com.perfect.IndiExport.dto.LoginResponse;
import com.perfect.IndiExport.dto.RegisterRequest;
import com.perfect.IndiExport.entity.Role;
import com.perfect.IndiExport.entity.User;
import com.perfect.IndiExport.repository.UserRepository;
import com.perfect.IndiExport.entity.TokenBlacklist;
import com.perfect.IndiExport.repository.TokenBlacklistRepository;
import com.perfect.IndiExport.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    // ---------------- LOGIN ----------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));

            User user = userRepository.findByEmail(request.getEmail()).get();

            String token = jwtUtil.generateToken(
                    user.getEmail(), user.getRole().name());

            return ResponseEntity.ok(
                    new LoginResponse(token, user.getRole().name()));

        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }

    // ---------------- REGISTER / SIGN-UP ----------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // 1️⃣ Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Email already registered");
        }

        // 2️⃣ Allow only BUYER or SELLER_BASIC
        if (request.getRole() == Role.ADMIN) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Admin registration not allowed");
        }

        // 3️⃣ Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus("ACTIVE");

        userRepository.save(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var expirationDate = jwtUtil.extractExpiration(token);
                var expiresAt = LocalDateTime.ofInstant(expirationDate.toInstant(), ZoneId.systemDefault());

                TokenBlacklist blacklistEntry = TokenBlacklist.builder()
                        .token(token)
                        .blacklistedAt(LocalDateTime.now())
                        .expiresAt(expiresAt)
                        .build();

                tokenBlacklistRepository.save(blacklistEntry);
            } catch (Exception e) {
                // Ignore if token is already invalid
            }
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    public boolean validateToken(String token) {
        try {
            jwtUtil.extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return jwtUtil.extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return jwtUtil.extractAllClaims(token).get("role", String.class);
    }

    @GetMapping("/token")
    public ResponseEntity<?> getCurrentToken(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("No token found in request");
        }

        String token = header.substring(7);

        return ResponseEntity.ok(token);
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid Authorization header");
        }

        String token = header.substring(7);

        // 1️⃣ Check blacklist
        if (tokenBlacklistRepository.existsByToken(token)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Token is blacklisted (logged out)");
        }

        try {
            // 2️⃣ Validate signature + expiration
            boolean isValid = jwtUtil.validateToken(token);

            if (!isValid) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid or expired token");
            }

            // 3️⃣ Extract data
            String email = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            return ResponseEntity.ok(
                    Map.of(
                            "valid", true,
                            "email", email,
                            "role", role));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Token validation failed");
        }
    }

}
