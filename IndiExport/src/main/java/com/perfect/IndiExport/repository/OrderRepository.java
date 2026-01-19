package com.perfect.IndiExport.repository;

import com.perfect.IndiExport.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    List<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);
    Optional<Order> findByIdAndSellerId(Long id, Long sellerId);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, Order.OrderStatus status);
    List<Order> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, Order.OrderStatus status);
    Optional<Order> findByInquiryId(Long inquiryId);
    Optional<Order> findByRfqResponseId(Long rfqResponseId);
    long countByBuyerId(Long buyerId);
    long countBySellerId(Long sellerId);
}

