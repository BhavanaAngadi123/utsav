package com.utsav.concierge;

import com.utsav.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** AI concierge chat: send messages, get vendor recommendations. */
@RestController
@RequestMapping("/api/concierge")
@Tag(name = "Concierge")
public class ConciergeController {

  private final ConciergeService conciergeService;
  private final JwtService jwtService;

  public ConciergeController(ConciergeService conciergeService, JwtService jwtService) {
    this.conciergeService = conciergeService;
    this.jwtService = jwtService;
  }

  @GetMapping("/status")
  @Operation(summary = "Concierge availability (AI online vs offline mode)")
  public StatusView status() {
    return new StatusView(conciergeService.isAvailable());
  }

  @PostMapping("/sessions")
  @Operation(summary = "Start a new concierge chat session")
  public ResponseEntity<SessionView> start(
      @RequestHeader(value = "Authorization", required = false) String auth) {
    UUID customerId = customerIdFrom(auth);
    ConciergeSession session = conciergeService.startSession(customerId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new SessionView(session.getId().toString()));
  }

  @PostMapping("/sessions/{sessionId}/chat")
  @Operation(summary = "Send a chat message; get AI reply with vendor recommendations")
  public ConciergeService.ChatReply chat(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @PathVariable UUID sessionId,
      @RequestBody ChatRequest req) {
    UUID customerId = customerIdFrom(auth);
    return conciergeService.chat(sessionId, customerId, req.message());
  }

  @GetMapping("/sessions/{sessionId}/history")
  @Operation(summary = "Chat history for a session")
  public List<MessageView> history(@PathVariable UUID sessionId) {
    return conciergeService.history(sessionId).stream()
        .map(m -> new MessageView(m.getRole(), m.getContent()))
        .toList();
  }

  private UUID customerIdFrom(String auth) {
    if (auth != null && auth.startsWith("Bearer ")) {
      try {
        return jwtService.parseAccessToken(auth.substring(7)).userId();
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  public record StatusView(boolean aiAvailable) {}
  public record SessionView(String sessionId) {}
  public record ChatRequest(@NotBlank String message) {}
  public record MessageView(String role, String content) {}
}
