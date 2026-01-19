package com.perfect.IndiExport.dto;

import com.perfect.IndiExport.entity.Order;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDto {
    private Long id;
    private String orderNumber;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerBusinessName;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String shippingMethod;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private String currency;
    private BigDecimal convertedAmount;
    private String convertedCurrency;
    private Order.OrderSource source;
    private Order.OrderStatus status;
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryCountry;
    private String deliveryPincode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

