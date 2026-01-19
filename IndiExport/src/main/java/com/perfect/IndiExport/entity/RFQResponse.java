package com.perfect.IndiExport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rfq_responses", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"rfq_id", "seller_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RFQResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false)
    private BigDecimal offeredPrice;

    @Column(nullable = false)
    private String estimatedDeliveryTime; // e.g., "15-20 days", "4 weeks"

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResponseStatus status = ResponseStatus.SUBMITTED;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ResponseStatus {
        SUBMITTED,  // Quotation submitted
        IN_CHAT,    // Chat opened for negotiation
        ACCEPTED,   // Buyer accepted the deal
        DECLINED    // Buyer declined the deal
    }
}

