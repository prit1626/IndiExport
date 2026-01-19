package com.perfect.IndiExport.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private String category;
    private BigDecimal price; // Original price in INR
    private Integer minQuantity;
    private String description;
    private String imageUrl;
    private String status;
    // Stock Management Fields
    private Integer declaredStock;
    private Integer reservedStock;
    private Integer remainingStock; // Calculated
    private String stockStatus; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    // Selling Countries
    private List<String> sellingCountries; // List of country codes (e.g., ["US", "GB", "IN"])
    
    // Buyer-specific fields (currency conversion)
    private BigDecimal convertedPrice; // Price in buyer's currency
    private String currency; // Buyer's currency code
    private String currencySymbol; // Currency symbol ($, £, €, etc.)
    
    // Seller info (for buyer view)
    private Long sellerId;
    private String sellerBusinessName;
    
    // Buy Now feature
    private Boolean allowDirectBuy;
}
