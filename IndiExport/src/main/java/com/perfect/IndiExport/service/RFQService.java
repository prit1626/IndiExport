package com.perfect.IndiExport.service;

import com.perfect.IndiExport.dto.RFQDto;
import com.perfect.IndiExport.dto.RFQRequest;
import com.perfect.IndiExport.dto.RFQResponseDto;
import com.perfect.IndiExport.dto.RFQResponseRequest;
import com.perfect.IndiExport.entity.Buyer;
import com.perfect.IndiExport.entity.RFQ;
import com.perfect.IndiExport.entity.RFQResponse;
import com.perfect.IndiExport.entity.Seller;
import com.perfect.IndiExport.entity.User;
import com.perfect.IndiExport.repository.BuyerRepository;
import com.perfect.IndiExport.repository.RFQRepository;
import com.perfect.IndiExport.repository.RFQResponseRepository;
import com.perfect.IndiExport.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RFQService {

    private final RFQRepository rfqRepository;
    private final RFQResponseRepository rfqResponseRepository;
    private final SellerRepository sellerRepository;
    private final BuyerRepository buyerRepository;

    public List<RFQDto> getAvailableRFQs(User user) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        // Get all OPEN RFQs that haven't expired
        List<RFQ> rfqs = rfqRepository.findByStatusAndExpiryDateAfterOrderByCreatedAtDesc(
                RFQ.RFQStatus.OPEN,
                LocalDate.now()
        );

        return rfqs.stream()
                .map(rfq -> mapToDto(rfq, seller.getId()))
                .collect(Collectors.toList());
    }

    public RFQDto getRFQDetails(User user, Long rfqId) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        RFQ rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        // Check if RFQ is still open and not expired
        if (rfq.getStatus() != RFQ.RFQStatus.OPEN || rfq.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("RFQ is no longer available");
        }

        return mapToDto(rfq, seller.getId());
    }

    @Transactional
    public RFQResponseDto respondToRFQ(User user, Long rfqId, RFQResponseRequest request) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        RFQ rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        // Check if RFQ is still open
        if (rfq.getStatus() != RFQ.RFQStatus.OPEN) {
            throw new RuntimeException("RFQ is no longer open");
        }

        // Check if RFQ has expired
        if (rfq.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("RFQ has expired");
        }

        // Check if seller has already responded
        if (rfqResponseRepository.existsByRfqIdAndSellerId(rfqId, seller.getId())) {
            throw new RuntimeException("You have already responded to this RFQ");
        }

        // Create response
        RFQResponse response = RFQResponse.builder()
                .rfq(rfq)
                .seller(seller)
                .offeredPrice(request.getOfferedPrice())
                .estimatedDeliveryTime(request.getEstimatedDeliveryTime())
                .message(request.getMessage())
                .build();

        RFQResponse saved = rfqResponseRepository.save(response);
        return mapResponseToDto(saved);
    }

    public List<RFQResponseDto> getRFQResponses(Long rfqId) {
        List<RFQResponse> responses = rfqResponseRepository.findByRfqIdOrderByCreatedAtDesc(rfqId);
        return responses.stream()
                .map(this::mapResponseToDto)
                .collect(Collectors.toList());
    }

    public List<RFQResponseDto> getMyRFQResponses(User user) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        List<RFQResponse> responses = rfqResponseRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId());
        return responses.stream()
                .map(this::mapResponseToDto)
                .collect(Collectors.toList());
    }

    private RFQDto mapToDto(RFQ rfq, Long sellerId) {
        RFQDto dto = new RFQDto();
        dto.setId(rfq.getId());
        dto.setBuyerId(rfq.getBuyer().getId());
        dto.setBuyerName(rfq.getBuyer().getName());
        // Partially hide buyer country - show only first letter and last letter
        String country = rfq.getBuyer().getEmail() != null ? 
            extractCountryFromEmail(rfq.getBuyer().getEmail()) : "Unknown";
        dto.setBuyerCountry(country);
        dto.setProductRequirement(rfq.getProductRequirement());
        dto.setQuantity(rfq.getQuantity());
        dto.setDescription(rfq.getDescription());
        dto.setDeliveryCountry(rfq.getDeliveryCountry());
        dto.setExpiryDate(rfq.getExpiryDate());
        dto.setStatus(rfq.getStatus());
        dto.setCreatedAt(rfq.getCreatedAt());
        dto.setUpdatedAt(rfq.getUpdatedAt());

        // Check if current seller has responded
        boolean hasResponded = rfqResponseRepository.existsByRfqIdAndSellerId(rfq.getId(), sellerId);
        dto.setHasResponded(hasResponded);

        // Get response count for this RFQ
        long responseCount = rfqResponseRepository.countByRfqId(rfq.getId());
        dto.setResponseCount(responseCount);

        return dto;
    }

    private RFQResponseDto mapResponseToDto(RFQResponse response) {
        RFQResponseDto dto = new RFQResponseDto();
        dto.setId(response.getId());
        dto.setRfqId(response.getRfq().getId());
        dto.setSellerId(response.getSeller().getId());
        dto.setSellerBusinessName(response.getSeller().getBusinessName());
        dto.setSellerMode(response.getSeller().getSellerMode());
        dto.setOfferedPrice(response.getOfferedPrice());
        dto.setEstimatedDeliveryTime(response.getEstimatedDeliveryTime());
        dto.setMessage(response.getMessage());
        dto.setStatus(response.getStatus());
        dto.setCreatedAt(response.getCreatedAt());
        dto.setUpdatedAt(response.getUpdatedAt());
        return dto;
    }

    // Buyer methods
    public List<RFQDto> getBuyerRFQs(User user) {
        List<RFQ> rfqs = rfqRepository.findByBuyerIdOrderByCreatedAtDesc(user.getId());
        return rfqs.stream()
                .map(rfq -> mapToDtoForBuyer(rfq))
                .collect(Collectors.toList());
    }

    public List<RFQDto> getBuyerRFQsByStatus(User user, RFQ.RFQStatus status) {
        List<RFQ> rfqs = rfqRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(user.getId(), status);
        return rfqs.stream()
                .map(rfq -> mapToDtoForBuyer(rfq))
                .collect(Collectors.toList());
    }

    public RFQDto getBuyerRFQDetails(User user, Long rfqId) {
        RFQ rfq = rfqRepository.findByIdAndBuyerId(rfqId, user.getId())
                .orElseThrow(() -> new RuntimeException("RFQ not found or access denied"));
        return mapToDtoForBuyer(rfq);
    }

    private RFQDto mapToDtoForBuyer(RFQ rfq) {
        RFQDto dto = new RFQDto();
        dto.setId(rfq.getId());
        dto.setBuyerId(rfq.getBuyer().getId());
        dto.setBuyerName(rfq.getBuyer().getName());
        dto.setBuyerCountry(rfq.getDeliveryCountry()); // Use delivery country for buyer view
        dto.setProductRequirement(rfq.getProductRequirement());
        dto.setQuantity(rfq.getQuantity());
        dto.setDescription(rfq.getDescription());
        dto.setDeliveryCountry(rfq.getDeliveryCountry());
        dto.setExpiryDate(rfq.getExpiryDate());
        dto.setStatus(rfq.getStatus());
        dto.setCreatedAt(rfq.getCreatedAt());
        dto.setUpdatedAt(rfq.getUpdatedAt());

        // Get response count for this RFQ
        long responseCount = rfqResponseRepository.countByRfqId(rfq.getId());
        dto.setResponseCount(responseCount);
        dto.setHasResponded(false); // Not applicable for buyer view

        return dto;
    }

    // Buyer methods - Create, Update, Delete RFQ
    @Transactional
    public RFQDto createRFQ(User buyerUser, RFQRequest request) {
        // Get buyer profile
        Buyer buyer = buyerRepository.findByUserId(buyerUser.getId())
                .orElseThrow(() -> new RuntimeException("Buyer profile not found. Please complete your profile."));

        // Validate expiry date (must be in future)
        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Expiry date must be in the future");
        }

        // Use buyer's country as delivery country if not provided
        String deliveryCountry = request.getDeliveryCountry();
        if (deliveryCountry == null || deliveryCountry.isEmpty()) {
            deliveryCountry = buyer.getCountry();
        }

        RFQ rfq = RFQ.builder()
                .buyer(buyerUser)
                .productRequirement(request.getProductRequirement())
                .quantity(request.getQuantity())
                .description(request.getDescription())
                .deliveryCountry(deliveryCountry)
                .expiryDate(request.getExpiryDate())
                .status(RFQ.RFQStatus.OPEN)
                .build();

        RFQ saved = rfqRepository.save(rfq);
        return mapToDtoForBuyer(saved);
    }

    @Transactional
    public RFQDto updateRFQ(User buyerUser, Long rfqId, RFQRequest request) {
        RFQ rfq = rfqRepository.findByIdAndBuyerId(rfqId, buyerUser.getId())
                .orElseThrow(() -> new RuntimeException("RFQ not found or access denied"));

        // Can only edit if no responses yet
        long responseCount = rfqResponseRepository.countByRfqId(rfqId);
        if (responseCount > 0) {
            throw new RuntimeException("Cannot edit RFQ. Sellers have already responded.");
        }

        // Can only edit if still OPEN
        if (rfq.getStatus() != RFQ.RFQStatus.OPEN) {
            throw new RuntimeException("Cannot edit closed RFQ");
        }

        // Update fields
        if (request.getProductRequirement() != null) rfq.setProductRequirement(request.getProductRequirement());
        if (request.getQuantity() != null) rfq.setQuantity(request.getQuantity());
        if (request.getDescription() != null) rfq.setDescription(request.getDescription());
        if (request.getDeliveryCountry() != null) rfq.setDeliveryCountry(request.getDeliveryCountry());
        if (request.getExpiryDate() != null) {
            if (request.getExpiryDate().isBefore(LocalDate.now())) {
                throw new RuntimeException("Expiry date must be in the future");
            }
            rfq.setExpiryDate(request.getExpiryDate());
        }

        RFQ updated = rfqRepository.save(rfq);
        return mapToDtoForBuyer(updated);
    }

    @Transactional
    public void deleteRFQ(User buyerUser, Long rfqId) {
        RFQ rfq = rfqRepository.findByIdAndBuyerId(rfqId, buyerUser.getId())
                .orElseThrow(() -> new RuntimeException("RFQ not found or access denied"));

        // Can only delete if no responses yet
        long responseCount = rfqResponseRepository.countByRfqId(rfqId);
        if (responseCount > 0) {
            throw new RuntimeException("Cannot delete RFQ. Sellers have already responded.");
        }

        rfqRepository.delete(rfq);
    }

    // RFQ Negotiation Methods
    @Transactional
    public RFQResponseDto acceptRFQResponse(User buyerUser, Long rfqResponseId) {
        RFQResponse response = rfqResponseRepository.findById(rfqResponseId)
                .orElseThrow(() -> new RuntimeException("RFQ response not found"));

        RFQ rfq = response.getRfq();

        // Verify buyer owns the RFQ
        if (!rfq.getBuyer().getId().equals(buyerUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Verify RFQ is still open or in negotiation
        if (rfq.getStatus() == RFQ.RFQStatus.CLOSED) {
            throw new RuntimeException("RFQ is already closed");
        }

        // Verify response is in chat (negotiation phase)
        if (response.getStatus() != RFQResponse.ResponseStatus.IN_CHAT) {
            throw new RuntimeException("RFQ response must be in chat/negotiation phase before acceptance");
        }

        // Accept this response
        response.setStatus(RFQResponse.ResponseStatus.ACCEPTED);
        rfqResponseRepository.save(response);

        // Decline all other responses
        List<RFQResponse> otherResponses = rfqResponseRepository.findByRfqIdOrderByCreatedAtDesc(rfq.getId())
                .stream()
                .filter(r -> !r.getId().equals(rfqResponseId))
                .collect(Collectors.toList());

        for (RFQResponse otherResponse : otherResponses) {
            if (otherResponse.getStatus() != RFQResponse.ResponseStatus.DECLINED) {
                otherResponse.setStatus(RFQResponse.ResponseStatus.DECLINED);
                rfqResponseRepository.save(otherResponse);
            }
        }

        // Close RFQ
        rfq.setStatus(RFQ.RFQStatus.CLOSED);
        rfq.setAcceptedResponse(response);
        rfqRepository.save(rfq);

        // Close all chat rooms for this RFQ
        // This will be handled by ChatService

        return mapResponseToDto(response);
    }

    @Transactional
    public RFQResponseDto declineRFQResponse(User buyerUser, Long rfqResponseId) {
        RFQResponse response = rfqResponseRepository.findById(rfqResponseId)
                .orElseThrow(() -> new RuntimeException("RFQ response not found"));

        RFQ rfq = response.getRfq();

        // Verify buyer owns the RFQ
        if (!rfq.getBuyer().getId().equals(buyerUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Verify RFQ is not closed
        if (rfq.getStatus() == RFQ.RFQStatus.CLOSED) {
            throw new RuntimeException("RFQ is already closed");
        }

        // Decline this response
        response.setStatus(RFQResponse.ResponseStatus.DECLINED);
        rfqResponseRepository.save(response);

        // Close chat for this response
        // This will be handled by ChatService

        return mapResponseToDto(response);
    }

    @Transactional
    public void startRFQNegotiation(User buyerUser, Long rfqResponseId) {
        RFQResponse response = rfqResponseRepository.findById(rfqResponseId)
                .orElseThrow(() -> new RuntimeException("RFQ response not found"));

        RFQ rfq = response.getRfq();

        // Verify buyer owns the RFQ
        if (!rfq.getBuyer().getId().equals(buyerUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Verify response is submitted
        if (response.getStatus() != RFQResponse.ResponseStatus.SUBMITTED) {
            throw new RuntimeException("RFQ response must be in SUBMITTED status");
        }

        // Update RFQ to NEGOTIATION if still OPEN
        if (rfq.getStatus() == RFQ.RFQStatus.OPEN) {
            rfq.setStatus(RFQ.RFQStatus.NEGOTIATION);
            rfqRepository.save(rfq);
        }

        // Update response status to IN_CHAT
        response.setStatus(RFQResponse.ResponseStatus.IN_CHAT);
        rfqResponseRepository.save(response);
    }

    private String extractCountryFromEmail(String email) {
        // Simple extraction - in real scenario, you might have buyer country in user profile
        // For now, return a placeholder
        return "International";
    }
}

