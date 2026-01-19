package com.perfect.IndiExport.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    private Long productId;
    private Integer quantity;
    private String shippingMethod;
    private BigDecimal shippingCost;
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryCountry;
    private String deliveryPincode;
}

