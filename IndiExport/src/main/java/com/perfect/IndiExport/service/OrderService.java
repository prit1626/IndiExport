package com.perfect.IndiExport.service;

import com.perfect.IndiExport.dto.OrderDto;
import com.perfect.IndiExport.dto.OrderRequest;
import com.perfect.IndiExport.entity.*;
import com.perfect.IndiExport.repository.*;
import com.perfect.IndiExport.service.InvoiceService;
import com.perfect.IndiExport.util.CurrencyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final InquiryRepository inquiryRepository;
    private final RFQResponseRepository rfqResponseRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public OrderDto createOrderFromBuyNow(User buyerUser, OrderRequest request) {
        // Get buyer profile
        Buyer buyer = buyerRepository.findByUserId(buyerUser.getId())
                .orElseThrow(() -> new RuntimeException("Buyer profile not found"));

        // Get product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Verify product allows direct buy
        if (!product.getAllowDirectBuy()) {
            throw new RuntimeException("This product does not allow direct purchase. Please use Inquiry.");
        }

        // Verify stock
        int remainingStock = product.getRemainingStock();
        if (remainingStock < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock. Available: " + remainingStock);
        }

        // Verify minimum quantity
        if (request.getQuantity() < product.getMinQuantity()) {
            throw new RuntimeException("Minimum quantity required: " + product.getMinQuantity());
        }

        // Get seller
        Seller seller = product.getSeller();

        // Calculate prices
        BigDecimal unitPrice = product.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
        BigDecimal shippingCost = request.getShippingCost() != null ? request.getShippingCost() : BigDecimal.ZERO;
        BigDecimal totalAmount = totalPrice.add(shippingCost);

        // Currency conversion
        String buyerCurrency = buyer.getCurrency() != null ? buyer.getCurrency() : "USD";
        BigDecimal convertedAmount = CurrencyUtil.convertFromINR(totalAmount, buyerCurrency);

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Create order
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .buyer(buyerUser)
                .seller(seller)
                .product(product)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .shippingMethod(request.getShippingMethod())
                .shippingCost(shippingCost)
                .totalAmount(totalAmount)
                .currency("INR")
                .convertedAmount(convertedAmount)
                .convertedCurrency(buyerCurrency)
                .source(Order.OrderSource.BUY_NOW)
                .status(Order.OrderStatus.CREATED)
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryCity(request.getDeliveryCity())
                .deliveryState(request.getDeliveryState())
                .deliveryCountry(request.getDeliveryCountry())
                .deliveryPincode(request.getDeliveryPincode())
                .build();

        // Deduct stock immediately
        product.setDeclaredStock(product.getDeclaredStock() - request.getQuantity());
        productRepository.save(product);

        Order saved = orderRepository.save(order);

        // Auto-generate invoice for Buy Now
        try {
            invoiceService.autoGenerateInvoiceForOrder(saved);
        } catch (Exception e) {
            // Log error but don't fail order creation
            System.err.println("Failed to auto-generate invoice: " + e.getMessage());
        }

        return mapToDto(saved);
    }

    @Transactional
    public OrderDto createOrderFromInquiry(User sellerUser, Long inquiryId) {
        Seller seller = sellerRepository.findById(sellerUser.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found"));

        // Verify inquiry belongs to seller
        if (!inquiry.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Check if order already exists
        if (orderRepository.findByInquiryId(inquiryId).isPresent()) {
            throw new RuntimeException("Order already exists for this inquiry");
        }

        Product product = inquiry.getProduct();
        Buyer buyer = buyerRepository.findByUserId(inquiry.getBuyer().getId())
                .orElseThrow(() -> new RuntimeException("Buyer profile not found"));

        // Calculate prices
        BigDecimal unitPrice = product.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(inquiry.getRequestedQuantity()));
        BigDecimal shippingCost = BigDecimal.ZERO; // Can be set later
        BigDecimal totalAmount = totalPrice.add(shippingCost);

        // Currency conversion
        String buyerCurrency = buyer.getCurrency() != null ? buyer.getCurrency() : "USD";
        BigDecimal convertedAmount = CurrencyUtil.convertFromINR(totalAmount, buyerCurrency);

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Create order
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .buyer(inquiry.getBuyer())
                .seller(seller)
                .product(product)
                .quantity(inquiry.getRequestedQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .shippingMethod(inquiry.getShippingOption())
                .shippingCost(shippingCost)
                .totalAmount(totalAmount)
                .currency("INR")
                .convertedAmount(convertedAmount)
                .convertedCurrency(buyerCurrency)
                .source(Order.OrderSource.INQUIRY)
                .inquiry(inquiry)
                .status(Order.OrderStatus.CREATED)
                .deliveryCountry(buyer.getCountry())
                .build();

        // Finalize stock deduction (was reserved, now deducted)
        int reserved = product.getReservedStock();
        int toDeduct = inquiry.getRequestedQuantity();
        product.setReservedStock(Math.max(0, reserved - toDeduct));
        product.setDeclaredStock(Math.max(0, product.getDeclaredStock() - toDeduct));
        productRepository.save(product);

        Order saved = orderRepository.save(order);
        return mapToDto(saved);
    }

    @Transactional
    public OrderDto createOrderFromRFQ(User buyerUser, Long rfqResponseId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUser.getId())
                .orElseThrow(() -> new RuntimeException("Buyer profile not found"));

        RFQResponse response = rfqResponseRepository.findById(rfqResponseId)
                .orElseThrow(() -> new RuntimeException("RFQ response not found"));

        // Verify response belongs to buyer's RFQ
        if (!response.getRfq().getBuyer().getId().equals(buyerUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Verify response is accepted
        if (response.getStatus() != RFQResponse.ResponseStatus.ACCEPTED) {
            throw new RuntimeException("RFQ response must be accepted before creating order");
        }

        RFQ rfq = response.getRfq();
        Seller seller = response.getSeller();

        // For RFQ, we need to create a product reference or use the negotiated price
        // For now, we'll create order with RFQ details
        BigDecimal unitPrice = response.getOfferedPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(rfq.getQuantity()));
        BigDecimal shippingCost = BigDecimal.ZERO;
        BigDecimal totalAmount = totalPrice.add(shippingCost);

        String buyerCurrency = buyer.getCurrency() != null ? buyer.getCurrency() : "USD";
        BigDecimal convertedAmount = CurrencyUtil.convertFromINR(totalAmount, buyerCurrency);

        String orderNumber = generateOrderNumber();

        // For RFQ, try to find a matching product or create order without product
        // In a real scenario, you might want to link RFQ to a product
        Product product = null; // RFQ orders may not have specific product
        
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .buyer(buyerUser)
                .seller(seller)
                .product(product) // RFQ orders may not have specific product
                .quantity(rfq.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .shippingCost(shippingCost)
                .totalAmount(totalAmount)
                .currency("INR")
                .convertedAmount(convertedAmount)
                .convertedCurrency(buyerCurrency)
                .source(Order.OrderSource.RFQ)
                .rfqResponse(response)
                .status(Order.OrderStatus.CREATED)
                .deliveryCountry(rfq.getDeliveryCountry())
                .build();

        Order saved = orderRepository.save(order);
        return mapToDto(saved);
    }

    public List<OrderDto> getBuyerOrders(User buyerUser) {
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerUser.getId());
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<OrderDto> getSellerOrders(User sellerUser) {
        Seller seller = sellerRepository.findById(sellerUser.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        List<Order> orders = orderRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId());
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public OrderDto getOrderDetails(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify ownership
        boolean isBuyer = order.getBuyer().getId().equals(user.getId());
        boolean isSeller = order.getSeller().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("ADMIN");

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new RuntimeException("Access denied");
        }

        return mapToDto(order);
    }

    @Transactional
    public OrderDto updateOrderStatus(User sellerUser, Long orderId, Order.OrderStatus status) {
        Seller seller = sellerRepository.findById(sellerUser.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        Order order = orderRepository.findByIdAndSellerId(orderId, seller.getId())
                .orElseThrow(() -> new RuntimeException("Order not found or access denied"));

        order.setStatus(status);
        Order updated = orderRepository.save(order);
        return mapToDto(updated);
    }

    private String generateOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = orderRepository.count() + 1;
        return String.format("ORD-%s-%04d", datePrefix, count);
    }

    private OrderDto mapToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setBuyerId(order.getBuyer().getId());
        dto.setBuyerName(order.getBuyer().getName());
        dto.setSellerId(order.getSeller().getId());
        dto.setSellerBusinessName(order.getSeller().getBusinessName());
        if (order.getProduct() != null) {
            dto.setProductId(order.getProduct().getId());
            dto.setProductName(order.getProduct().getName());
        } else {
            // For RFQ orders, product might be null
            dto.setProductName("Custom Product (RFQ)");
        }
        dto.setQuantity(order.getQuantity());
        dto.setUnitPrice(order.getUnitPrice());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setShippingMethod(order.getShippingMethod());
        dto.setShippingCost(order.getShippingCost());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCurrency(order.getCurrency());
        dto.setConvertedAmount(order.getConvertedAmount());
        dto.setConvertedCurrency(order.getConvertedCurrency());
        dto.setSource(order.getSource());
        dto.setStatus(order.getStatus());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setDeliveryCity(order.getDeliveryCity());
        dto.setDeliveryState(order.getDeliveryState());
        dto.setDeliveryCountry(order.getDeliveryCountry());
        dto.setDeliveryPincode(order.getDeliveryPincode());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }
}

