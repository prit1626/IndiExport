package com.perfect.IndiExport.repository;

import com.perfect.IndiExport.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrderId(Long orderId);
    
    List<Invoice> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    
    List<Invoice> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    long countBySellerId(Long sellerId);
    
    long countByBuyerId(Long buyerId);
}



