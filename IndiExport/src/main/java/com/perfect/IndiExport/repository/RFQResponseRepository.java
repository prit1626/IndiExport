package com.perfect.IndiExport.repository;

import com.perfect.IndiExport.entity.RFQResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RFQResponseRepository extends JpaRepository<RFQResponse, Long> {
    List<RFQResponse> findByRfqIdOrderByCreatedAtDesc(Long rfqId);
    
    List<RFQResponse> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    
    Optional<RFQResponse> findByRfqIdAndSellerId(Long rfqId, Long sellerId);
    
    boolean existsByRfqIdAndSellerId(Long rfqId, Long sellerId);
    
    long countByRfqId(Long rfqId);
    
    long countBySellerId(Long sellerId);
    
    List<RFQResponse> findByRfqIdAndStatusOrderByCreatedAtDesc(Long rfqId, RFQResponse.ResponseStatus status);
    
    Optional<RFQResponse> findByIdAndRfqBuyerId(Long id, Long buyerId);
}

