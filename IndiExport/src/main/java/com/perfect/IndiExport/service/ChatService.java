package com.perfect.IndiExport.service;

import com.perfect.IndiExport.dto.ChatMessageDto;
import com.perfect.IndiExport.dto.ChatRoomDto;
import com.perfect.IndiExport.dto.SendMessageRequest;
import com.perfect.IndiExport.entity.*;
import com.perfect.IndiExport.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final InquiryRepository inquiryRepository;
    private final RFQResponseRepository rfqResponseRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<ChatRoomDto> getSellerChatRooms(User user) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        List<ChatRoom> rooms = chatRoomRepository.findBySellerIdOrderByUpdatedAtDesc(seller.getId());
        return rooms.stream()
                .map(room -> mapToDto(room, user))
                .collect(Collectors.toList());
    }

    public List<ChatRoomDto> getBuyerChatRooms(User user) {
        List<ChatRoom> rooms = chatRoomRepository.findByBuyerIdOrderByUpdatedAtDesc(user.getId());
        return rooms.stream()
                .map(room -> mapToDto(room, user))
                .collect(Collectors.toList());
    }

    public ChatRoomDto getOrCreateChatRoom(User user, Long inquiryId) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found"));

        // Verify inquiry belongs to seller
        if (!inquiry.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Get or create chat room
        ChatRoom room = chatRoomRepository.findByInquiryId(inquiryId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .inquiry(inquiry)
                            .buyer(inquiry.getBuyer())
                            .seller(seller)
                            .isActive(true)
                            .build();
                    return chatRoomRepository.save(newRoom);
                });

        return mapToDto(room, user);
    }

    public List<ChatMessageDto> getChatMessages(User user, Long chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        // Verify access
        boolean isOwner = false;
        if (user.getRole().name().contains("SELLER")) {
            Seller seller = sellerRepository.findById(user.getId()).orElse(null);
            if (seller != null && room.getSeller().getId().equals(seller.getId())) {
                isOwner = true;
            }
        } else {
            if (room.getBuyer().getId().equals(user.getId())) {
                isOwner = true;
            }
        }

        if (!isOwner) {
            throw new RuntimeException("Access denied. You are not a participant in this chat room.");
        }

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);
        return messages.stream()
                .map(this::mapMessageToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto sendMessage(User user, Long chatRoomId, SendMessageRequest request, boolean isSeller) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        // Verify access
        if (isSeller) {
            Seller seller = sellerRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Seller profile not found"));
            if (!room.getSeller().getId().equals(seller.getId())) {
                throw new RuntimeException("Access denied");
            }
        } else {
            if (!room.getBuyer().getId().equals(user.getId())) {
                throw new RuntimeException("Access denied");
            }
        }

        // Check if seller is BASIC and trying to send file
        if (isSeller && (request.getFileName() != null || request.getFileUrl() != null)) {
            Seller seller = sellerRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Seller profile not found"));
            if ("BASIC".equals(seller.getSellerMode())) {
                throw new RuntimeException(
                        "File sharing is only available for ADVANCED sellers. Please upgrade to use this feature.");
            }
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(user)
                .senderType(isSeller ? ChatMessage.MessageType.SELLER : ChatMessage.MessageType.BUYER)
                .message(request.getMessage())
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .isRead(false)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        // Update room's updatedAt
        room.setUpdatedAt(java.time.LocalDateTime.now());
        chatRoomRepository.save(room);

        ChatMessageDto messageDto = mapMessageToDto(saved);

        // Send via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, messageDto);

        return messageDto;
    }

    @Transactional
    public void markMessagesAsRead(User user, Long chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        // Determine message type to mark as read (opposite of current user)
        ChatMessage.MessageType targetType = room.getBuyer().getId().equals(user.getId())
                ? ChatMessage.MessageType.SELLER
                : ChatMessage.MessageType.BUYER;

        List<ChatMessage> unreadMessages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId)
                .stream()
                .filter(msg -> msg.getSenderType() == targetType && !msg.getIsRead())
                .collect(Collectors.toList());

        unreadMessages.forEach(msg -> msg.setIsRead(true));
        chatMessageRepository.saveAll(unreadMessages);
    }

    private ChatRoomDto mapToDto(ChatRoom room, User currentUser) {
        ChatRoomDto dto = new ChatRoomDto();
        dto.setId(room.getId());
        dto.setBuyerId(room.getBuyer().getId());
        dto.setBuyerName(room.getBuyer().getName());
        dto.setSellerId(room.getSeller().getId());
        dto.setSellerBusinessName(room.getSeller().getBusinessName());
        dto.setIsActive(room.getIsActive());
        dto.setCreatedAt(room.getCreatedAt());
        dto.setUpdatedAt(room.getUpdatedAt());

        // Handle Inquiry-based chat
        if (room.getInquiry() != null) {
            dto.setInquiryId(room.getInquiry().getId());
            dto.setProductId(room.getInquiry().getProduct().getId());
            dto.setProductName(room.getInquiry().getProduct().getName());
        }

        // Handle RFQ-based chat
        if (room.getRfqResponse() != null) {
            dto.setRfqResponseId(room.getRfqResponse().getId());
            dto.setRfqId(room.getRfqResponse().getRfq().getId());
            dto.setProductName("RFQ: " + room.getRfqResponse().getRfq().getProductRequirement());
        }

        // Get unread count - count messages from the other party
        boolean isBuyer = room.getBuyer().getId().equals(currentUser.getId());
        ChatMessage.MessageType targetType = isBuyer
                ? ChatMessage.MessageType.SELLER
                : ChatMessage.MessageType.BUYER;
        long unreadCount = chatMessageRepository.countByChatRoomIdAndIsReadFalseAndSenderType(
                room.getId(), targetType);
        dto.setUnreadCount(unreadCount);

        // Get last message time
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId());
        if (!messages.isEmpty()) {
            dto.setLastMessageAt(messages.get(messages.size() - 1).getCreatedAt());
        }

        return dto;
    }

    private ChatMessageDto mapMessageToDto(ChatMessage message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(message.getId());
        dto.setChatRoomId(message.getChatRoom().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getName());
        dto.setSenderType(message.getSenderType());
        dto.setMessage(message.getMessage());
        dto.setFileName(message.getFileName());
        dto.setFileUrl(message.getFileUrl());
        dto.setIsRead(message.getIsRead());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    public ChatRoomDto getOrCreateRFQChatRoom(User user, Long rfqResponseId) {

        RFQResponse rfqResponse = rfqResponseRepository.findById(rfqResponseId)
                .orElseThrow(() -> new RuntimeException("RFQ Response not found"));

        // Access control
        boolean isSeller = user.getRole().name().contains("SELLER");

        if (isSeller) {
            Seller seller = sellerRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Seller profile not found"));

            if (!rfqResponse.getSeller().getId().equals(seller.getId())) {
                throw new RuntimeException("Access denied");
            }
        } else {
            if (!rfqResponse.getRfq().getBuyer().getId().equals(user.getId())) {
                throw new RuntimeException("Access denied");
            }
        }

        // Get or create chat room
        ChatRoom room = chatRoomRepository.findByRfqResponseId(rfqResponseId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .rfqResponse(rfqResponse)
                            .buyer(rfqResponse.getRfq().getBuyer())
                            .seller(rfqResponse.getSeller())
                            .isActive(true)
                            .build();
                    return chatRoomRepository.save(newRoom);
                });

        return mapToDto(room, user);
    }

}
