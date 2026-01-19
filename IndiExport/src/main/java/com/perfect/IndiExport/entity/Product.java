package com.perfect.IndiExport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer minQuantity;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, DRAFT

    // Stock Management Fields
    @Column(nullable = false)
    @Builder.Default
    private Integer declaredStock = 0; // Total quantity seller offers

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedStock = 0; // Quantity locked by inquiries/invoices

    @Column(name = "allow_direct_buy", nullable = false)
    @Builder.Default
    private Boolean allowDirectBuy = false; // Allow direct purchase without inquiry

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductSellingCountry> sellingCountries = new ArrayList<>();

    // Calculated field (not stored in DB)
    public Integer getRemainingStock() {
        return Math.max(0, declaredStock - reservedStock);
    }

    public String getStockStatus() {
        int remaining = getRemainingStock();
        if (remaining == 0)
            return "OUT_OF_STOCK";
        if (remaining <= 10)
            return "LOW_STOCK";
        return "IN_STOCK";
    }
}
