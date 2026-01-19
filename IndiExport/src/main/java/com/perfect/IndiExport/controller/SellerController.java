package com.perfect.IndiExport.controller;

import com.perfect.IndiExport.dto.SellerOnboardingRequest;
import com.perfect.IndiExport.entity.Seller;
import com.perfect.IndiExport.entity.User;
import com.perfect.IndiExport.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.perfect.IndiExport.repository.UserRepository;

@RestController
@RequestMapping("/api/seller")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;
    private final UserRepository userRepository;

    @PostMapping("/onboard")
    public ResponseEntity<Seller> onboardSeller(@RequestBody SellerOnboardingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Fetch full user entity from DB using email/username from details
        // Assuming CustomUserDetailsService returns a Principal that might be User or
        // we fetch by email
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Seller seller = sellerService.onboardSeller(user, request);
        return ResponseEntity.ok(seller);
    }

    @GetMapping("/profile")
    public ResponseEntity<Seller> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(sellerService.getSellerProfile(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<Seller> updateProfile(@RequestBody SellerOnboardingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Seller updatedSeller = sellerService.updateSellerProfile(user, request);
        return ResponseEntity.ok(updatedSeller);
    }
}
