package com.perfect.IndiExport.controller;

import com.perfect.IndiExport.dto.OrderDto;
import com.perfect.IndiExport.dto.OrderRequest;
import com.perfect.IndiExport.entity.Order;
import com.perfect.IndiExport.entity.User;
import com.perfect.IndiExport.repository.UserRepository;
import com.perfect.IndiExport.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping("/buy-now")
    public ResponseEntity<OrderDto> createBuyNowOrder(
            @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is buyer
        if (!user.getRole().name().equals("BUYER")) {
            throw new RuntimeException("Only buyers can create Buy Now orders");
        }

        OrderDto order = orderService.createOrderFromBuyNow(user, request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/from-inquiry/{inquiryId}")
    public ResponseEntity<OrderDto> createOrderFromInquiry(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is seller
        if (!user.getRole().name().contains("SELLER")) {
            throw new RuntimeException("Only sellers can create orders from inquiries");
        }

        OrderDto order = orderService.createOrderFromInquiry(user, inquiryId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/from-rfq/{rfqResponseId}")
    public ResponseEntity<OrderDto> createOrderFromRFQ(
            @PathVariable Long rfqResponseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is buyer
        if (!user.getRole().name().equals("BUYER")) {
            throw new RuntimeException("Only buyers can create orders from RFQ");
        }

        OrderDto order = orderService.createOrderFromRFQ(user, rfqResponseId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<OrderDto> orders;
        boolean isSeller = user.getRole().name().contains("SELLER");

        if (isSeller) {
            orders = orderService.getSellerOrders(user);
        } else {
            orders = orderService.getBuyerOrders(user);
        }

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        OrderDto order = orderService.getOrderDetails(user, id);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is seller
        if (!user.getRole().name().contains("SELLER")) {
            throw new RuntimeException("Only sellers can update order status");
        }

        OrderDto order = orderService.updateOrderStatus(user, id, status);
        return ResponseEntity.ok(order);
    }
}

