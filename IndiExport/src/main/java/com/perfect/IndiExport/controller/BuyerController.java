package com.perfect.IndiExport.controller;

import com.perfect.IndiExport.dto.BuyerProfileDto;
import com.perfect.IndiExport.entity.Buyer;
import com.perfect.IndiExport.entity.User;
import com.perfect.IndiExport.repository.UserRepository;
import com.perfect.IndiExport.service.BuyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buyer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUYER')")
@CrossOrigin
public class BuyerController {

    private final UserRepository userRepository;
    private final BuyerService buyerService;

    @GetMapping("/profile")
    public ResponseEntity<Buyer> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Buyer buyer = buyerService.getBuyerProfile(user);
        return ResponseEntity.ok(buyer);
    }

    @PostMapping("/profile")
    public ResponseEntity<Buyer> createOrUpdateProfile(
            @RequestBody BuyerProfileDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Buyer buyer = buyerService.createOrUpdateBuyerProfile(user, dto);
        return ResponseEntity.ok(buyer);
    }

    @PutMapping("/profile")
    public ResponseEntity<Buyer> updateProfile(
            @RequestBody BuyerProfileDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Buyer buyer = buyerService.createOrUpdateBuyerProfile(user, dto);
        return ResponseEntity.ok(buyer);
    }

    @GetMapping("/currencies")
    public ResponseEntity<java.util.List<String>> getCurrencies() {
        return ResponseEntity.ok(com.perfect.IndiExport.util.CurrencyUtil.getSupportedCurrencies());
    }
}
