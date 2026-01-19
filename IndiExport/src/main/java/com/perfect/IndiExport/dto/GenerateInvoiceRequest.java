package com.perfect.IndiExport.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GenerateInvoiceRequest {
    private Long orderId; // Invoice is generated from Order
    private BigDecimal shippingCost; // Can override order shipping cost
    private String shippingMethod; // Can override order shipping method
    private String convertedCurrency; // Optional, for currency conversion
}



