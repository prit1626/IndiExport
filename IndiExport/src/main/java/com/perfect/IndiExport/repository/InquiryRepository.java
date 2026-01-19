package com.perfect.IndiExport.repository;

import com.perfect.IndiExport.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    
    Optional<Inquiry> findByIdAndSellerId(Long id, Long sellerId);
    
    List<Inquiry> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, Inquiry.InquiryStatus status);
    
    long countBySellerId(Long sellerId);
    
    // Buyer methods
    List<Inquiry> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    
    List<Inquiry> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, Inquiry.InquiryStatus status);
    
    Optional<Inquiry> findByIdAndBuyerId(Long id, Long buyerId);
    
    long countByBuyerId(Long buyerId);

    
}



