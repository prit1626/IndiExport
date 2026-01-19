package com.perfect.IndiExport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber; // Auto-generated: ORD-YYYYMMDD-XXXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    private String shippingMethod;
    private BigDecimal shippingCost;

    @Column(nullable = false)
    private BigDecimal totalAmount; // totalPrice + shippingCost

    @Column(nullable = false)
    private String currency = "INR";

    private BigDecimal convertedAmount; // Converted to buyer's currency
    private String convertedCurrency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderSource source = OrderSource.INQUIRY;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id")
    private Inquiry inquiry; // Null if source is BUY_NOW

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_response_id")
    private RFQResponse rfqResponse; // Null if not from RFQ

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryCountry;
    private String deliveryPincode;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum OrderSource {
        INQUIRY,    // Order created from inquiry flow
        BUY_NOW,    // Order created from direct buy flow
        RFQ         // Order created from RFQ acceptance
    }

    public enum OrderStatus {
        CREATED,    // Order created, pending confirmation
        CONFIRMED,  // Order confirmed by seller
        SHIPPED,    // Order shipped
        DELIVERED,  // Order delivered
        CANCELLED,  // Order cancelled
        CLOSED      // Order closed/completed
    }
}

