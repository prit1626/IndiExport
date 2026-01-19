package com.perfect.IndiExport.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.perfect.IndiExport.entity.RFQResponse;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RFQResponseDto {
    private Long id;
    private Long rfqId;
    private Long sellerId;
    private String sellerBusinessName;
    private String sellerMode; // BASIC or ADVANCED
    private BigDecimal offeredPrice;
    private String estimatedDeliveryTime;
    private String message;
    private RFQResponse.ResponseStatus status; // SUBMITTED, IN_CHAT, ACCEPTED, DECLINED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



