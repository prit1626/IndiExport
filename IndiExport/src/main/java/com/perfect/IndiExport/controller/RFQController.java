package com.perfect.IndiExport.controller;

import com.perfect.IndiExport.dto.RFQDto;
import com.perfect.IndiExport.dto.RFQRequest;
import com.perfect.IndiExport.dto.RFQResponseDto;
import com.perfect.IndiExport.dto.RFQResponseRequest;
import com.perfect.IndiExport.entity.User;
import com.perfect.IndiExport.repository.UserRepository;
import com.perfect.IndiExport.service.RFQService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rfqs")
@RequiredArgsConstructor
@CrossOrigin
public class RFQController {

    private final RFQService rfqService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<RFQDto>> getAvailableRFQs(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<RFQDto> rfqs;
        boolean isSeller = user.getRole().name().contains("SELLER");

        if (isSeller) {
            rfqs = rfqService.getAvailableRFQs(user);
        } else {
            // Buyer - get their own RFQs
            rfqs = rfqService.getBuyerRFQs(user);
        }

        return ResponseEntity.ok(rfqs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RFQDto> getRFQDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        RFQDto rfq;
        boolean isSeller = user.getRole().name().contains("SELLER");

        if (isSeller) {
            rfq = rfqService.getRFQDetails(user, id);
        } else {
            rfq = rfqService.getBuyerRFQDetails(user, id);
        }

        return ResponseEntity.ok(rfq);
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<RFQResponseDto> respondToRFQ(
            @PathVariable Long id,
            @RequestBody RFQResponseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        RFQResponseDto response = rfqService.respondToRFQ(user, id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/responses")
    public ResponseEntity<List<RFQResponseDto>> getRFQResponses(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isSeller = user.getRole().name().contains("SELLER");
        if (isSeller) {
            throw new RuntimeException("Access denied. Sellers cannot view all responses to an RFQ.");
        }

        // For buyers, verify they own the RFQ
        rfqService.getBuyerRFQDetails(user, id); // Throws exception if not owner

        List<RFQResponseDto> responses = rfqService.getRFQResponses(id);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my-responses")
    public ResponseEntity<List<RFQResponseDto>> getMyRFQResponses(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<RFQResponseDto> responses = rfqService.getMyRFQResponses(user);
        return ResponseEntity.ok(responses);
    }

    // Buyer endpoints
    @PostMapping
    public ResponseEntity<RFQDto> createRFQ(
            @RequestBody RFQRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        RFQDto rfq = rfqService.createRFQ(user, request);
        return ResponseEntity.ok(rfq);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RFQDto> updateRFQ(
            @PathVariable Long id,
            @RequestBody RFQRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        RFQDto rfq = rfqService.updateRFQ(user, id, request);
        return ResponseEntity.ok(rfq);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRFQ(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        rfqService.deleteRFQ(user, id);
        return ResponseEntity.ok("RFQ deleted successfully");
    }

    // RFQ Negotiation endpoints
    @PostMapping("/responses/{rfqResponseId}/start-chat")
    public ResponseEntity<?> startRFQNegotiation(
            @PathVariable Long rfqResponseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is buyer
        if (!user.getRole().name().equals("BUYER")) {
            throw new RuntimeException("Only buyers can start RFQ negotiation");
        }

        rfqService.startRFQNegotiation(user, rfqResponseId);
        return ResponseEntity.ok("RFQ negotiation started");
    }

    @PostMapping("/responses/{rfqResponseId}/accept")
    public ResponseEntity<RFQResponseDto> acceptRFQResponse(
            @PathVariable Long rfqResponseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is buyer
        if (!user.getRole().name().equals("BUYER")) {
            throw new RuntimeException("Only buyers can accept RFQ responses");
        }

        RFQResponseDto response = rfqService.acceptRFQResponse(user, rfqResponseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/responses/{rfqResponseId}/decline")
    public ResponseEntity<RFQResponseDto> declineRFQResponse(
            @PathVariable Long rfqResponseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is buyer
        if (!user.getRole().name().equals("BUYER")) {
            throw new RuntimeException("Only buyers can decline RFQ responses");
        }

        RFQResponseDto response = rfqService.declineRFQResponse(user, rfqResponseId);
        return ResponseEntity.ok(response);
    }
}
