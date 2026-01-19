package com.perfect.IndiExport.service;

import com.perfect.IndiExport.dto.GenerateInvoiceRequest;
import com.perfect.IndiExport.dto.InvoiceDto;
import com.perfect.IndiExport.entity.*;
import com.perfect.IndiExport.repository.*;
import com.perfect.IndiExport.util.InvoicePdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final InquiryRepository inquiryRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final InvoicePdfGenerator pdfGenerator;

    @Transactional
    public InvoiceDto generateInvoice(User user, GenerateInvoiceRequest request) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify order belongs to seller
        if (!order.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Check if invoice already exists
        if (invoiceRepository.findByOrderId(order.getId()).isPresent()) {
            throw new RuntimeException("Invoice already exists for this order");
        }

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Use order prices or override with request
        BigDecimal shippingCost = request.getShippingCost() != null ? request.getShippingCost() : order.getShippingCost();
        String shippingMethod = request.getShippingMethod() != null ? request.getShippingMethod() : order.getShippingMethod();
        BigDecimal totalAmount = order.getTotalPrice().add(shippingCost);

        // Currency conversion
        BigDecimal convertedAmount = order.getConvertedAmount();
        String convertedCurrency = request.getConvertedCurrency() != null ? request.getConvertedCurrency() : order.getConvertedCurrency();
        if (convertedCurrency != null && !convertedCurrency.equals("INR") && convertedAmount == null) {
            convertedAmount = convertCurrency(totalAmount, "INR", convertedCurrency);
        }

        // Create invoice
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .seller(seller)
                .buyer(order.getBuyer())
                .product(order.getProduct())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalPrice(order.getTotalPrice())
                .shippingMethod(shippingMethod)
                .shippingCost(shippingCost)
                .totalAmount(totalAmount)
                .currency(order.getCurrency())
                .convertedAmount(convertedAmount)
                .convertedCurrency(convertedCurrency)
                .status(Invoice.InvoiceStatus.DRAFT)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        return mapToDto(saved);
    }

    @Transactional
    public InvoiceDto autoGenerateInvoiceForOrder(Order order) {
        // Auto-generate invoice for Buy Now orders
        if (order.getSource() != Order.OrderSource.BUY_NOW) {
            throw new RuntimeException("Auto-invoice generation only for Buy Now orders");
        }

        // Check if invoice already exists
        if (invoiceRepository.findByOrderId(order.getId()).isPresent()) {
            return mapToDto(invoiceRepository.findByOrderId(order.getId()).get());
        }

        String invoiceNumber = generateInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .seller(order.getSeller())
                .buyer(order.getBuyer())
                .product(order.getProduct())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalPrice(order.getTotalPrice())
                .shippingMethod(order.getShippingMethod())
                .shippingCost(order.getShippingCost())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .convertedAmount(order.getConvertedAmount())
                .convertedCurrency(order.getConvertedCurrency())
                .status(Invoice.InvoiceStatus.CONFIRMED) // Auto-confirmed for Buy Now
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        return mapToDto(saved);
    }

    @Transactional
    public InvoiceDto confirmInvoice(User user, Long invoiceId) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Verify invoice belongs to seller
        if (!invoice.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT invoices can be confirmed");
        }

        // Stock is already deducted when order was created, so no need to deduct again
        // Just confirm the invoice

        // Update invoice status
        invoice.setStatus(Invoice.InvoiceStatus.CONFIRMED);
        Invoice updated = invoiceRepository.save(invoice);

        return mapToDto(updated);
    }

    @Transactional
    public InvoiceDto cancelInvoice(User user, Long invoiceId) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Verify invoice belongs to seller
        if (!invoice.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw new RuntimeException("Invoice is already cancelled");
        }

        // If confirmed, release stock
        if (invoice.getStatus() == Invoice.InvoiceStatus.CONFIRMED) {
            productService.releaseStock(invoice.getProduct().getId(), invoice.getQuantity());
        }

        // Update invoice status
        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        Invoice updated = invoiceRepository.save(invoice);

        return mapToDto(updated);
    }

    public InvoiceDto getInvoice(User user, Long invoiceId) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Verify invoice belongs to seller
        if (!invoice.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Access denied");
        }

        return mapToDto(invoice);
    }

    public List<InvoiceDto> getSellerInvoices(User user) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        List<Invoice> invoices = invoiceRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId());
        return invoices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public byte[] generatePdf(User user, Long invoiceId) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Verify invoice belongs to seller
        if (!invoice.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Verify seller is ADVANCED for file sharing (PDF Download)
        if (!"ADVANCED".equals(seller.getSellerMode())) {
            throw new RuntimeException(
                    "PDF download is only available for ADVANCED sellers. Please upgrade your profile.");
        }

        return pdfGenerator.generatePdf(invoice);
    }

    private String generateInvoiceNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = invoiceRepository.count() + 1;
        return "INV-" + datePrefix + "-" + String.format("%04d", count);
    }

    private BigDecimal convertCurrency(BigDecimal amount, String from, String to) {
        // Simplified currency conversion - in production, use real API like
        // ExchangeRate-API
        // For now, return a mock conversion
        if ("USD".equals(to)) {
            return amount.divide(BigDecimal.valueOf(83), 2, RoundingMode.HALF_UP); // Approximate 1 USD = 83 INR
        } else if ("EUR".equals(to)) {
            return amount.divide(BigDecimal.valueOf(90), 2, RoundingMode.HALF_UP); // Approximate 1 EUR = 90 INR
        }
        return amount;
    }

    private InvoiceDto mapToDto(Invoice invoice) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setOrderId(invoice.getOrder().getId());
        dto.setOrderNumber(invoice.getOrder().getOrderNumber());
        if (invoice.getOrder().getInquiry() != null) {
            dto.setInquiryId(invoice.getOrder().getInquiry().getId());
        }
        dto.setSellerId(invoice.getSeller().getId());
        dto.setSellerBusinessName(invoice.getSeller().getBusinessName());
        dto.setSellerGstNumber(invoice.getSeller().getGstNumber());
        dto.setSellerAddress(invoice.getSeller().getAddress() + ", " + invoice.getSeller().getCity() + ", "
                + invoice.getSeller().getState());
        dto.setBuyerId(invoice.getBuyer().getId());
        dto.setBuyerName(invoice.getBuyer().getName());
        dto.setBuyerEmail(invoice.getBuyer().getEmail());
        if (invoice.getOrder().getDeliveryCountry() != null) {
            dto.setBuyerCountry(invoice.getOrder().getDeliveryCountry());
        }
        if (invoice.getProduct() != null) {
            dto.setProductId(invoice.getProduct().getId());
            dto.setProductName(invoice.getProduct().getName());
            dto.setProductCategory(invoice.getProduct().getCategory());
        }
        dto.setQuantity(invoice.getQuantity());
        dto.setUnitPrice(invoice.getUnitPrice());
        dto.setTotalPrice(invoice.getTotalPrice());
        dto.setShippingMethod(invoice.getShippingMethod());
        dto.setShippingCost(invoice.getShippingCost());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setCurrency(invoice.getCurrency());
        dto.setConvertedAmount(invoice.getConvertedAmount());
        dto.setConvertedCurrency(invoice.getConvertedCurrency());
        dto.setStatus(invoice.getStatus());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());
        return dto;
    }
}
