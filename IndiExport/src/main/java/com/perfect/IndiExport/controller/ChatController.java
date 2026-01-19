package com.perfect.IndiExport.controller;

import com.perfect.IndiExport.dto.ChatMessageDto;
import com.perfect.IndiExport.dto.ChatRoomDto;
import com.perfect.IndiExport.dto.SendMessageRequest;
import com.perfect.IndiExport.entity.User;
import com.perfect.IndiExport.repository.UserRepository;
import com.perfect.IndiExport.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getChatRooms(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Determine if user is seller or buyer and return appropriate rooms
        boolean isSeller = user.getRole().name().contains("SELLER");
        List<ChatRoomDto> rooms = isSeller 
            ? chatService.getSellerChatRooms(user) 
            : chatService.getBuyerChatRooms(user);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/inquiry/{inquiryId}")
    public ResponseEntity<ChatRoomDto> getOrCreateChatRoom(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoomDto room = chatService.getOrCreateChatRoom(user, inquiryId);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/rooms/rfq/{rfqResponseId}")
    public ResponseEntity<ChatRoomDto> getOrCreateRFQChatRoom(
            @PathVariable Long rfqResponseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoomDto room = chatService.getOrCreateRFQChatRoom(user, rfqResponseId);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatMessageDto> messages = chatService.getChatMessages(user, roomId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable Long roomId,
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Determine if user is seller (simplified - check role)
        boolean isSeller = user.getRole().name().contains("SELLER");
        ChatMessageDto message = chatService.sendMessage(user, roomId, request, isSeller);
        return ResponseEntity.ok(message);
    }

    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        chatService.markMessagesAsRead(user, roomId);
        return ResponseEntity.ok("Messages marked as read");
    }
}



