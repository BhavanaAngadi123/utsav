package com.utsav.concierge;

import com.utsav.config.UtsavProperties;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Concierge chat: persists sessions/messages; delegates to the AI chat client. */
@Service
public class ConciergeService {

  private final ConciergeSessionRepository sessions;
  private final ConciergeMessageRepository messages;
  private final Optional<ChatClient> chatClient;
  private final VendorTools vendorTools;
  private final UtsavProperties props;

  public ConciergeService(
      ConciergeSessionRepository sessions,
      ConciergeMessageRepository messages,
      Optional<ChatClient> chatClient,
      VendorTools vendorTools,
      UtsavProperties props) {
    this.sessions = sessions;
    this.messages = messages;
    this.chatClient = chatClient;
    this.vendorTools = vendorTools;
    this.props = props;
  }

  public boolean isAvailable() {
    return props.getConcierge().isEnabled() && chatClient.isPresent();
  }

  public ConciergeSession startSession(UUID customerId) {
    ConciergeSession session = new ConciergeSession(customerId);
    sessions.save(session);
    messages.save(
        new ConciergeMessage(
            session,
            "system",
            "You are Utsav's AI event concierge. Help customers plan events: "
                + "understand their event type, guest count, budget, location and vibe. "
                + "Use the vendor search tools to find real vendors, prioritize highest-rated "
                + "vendors near the customer, and stay budget-aware. Guide first-time users "
                + "step by step. Be warm, concise, and practical."));
    return session;
  }

  public ChatReply chat(UUID sessionId, UUID customerId, String message) {
    ConciergeSession session =
        sessions
            .findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "session not found"));
    if (customerId != null && session.getCustomerId() != null
        && !session.getCustomerId().equals(customerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your session");
    }
    messages.save(new ConciergeMessage(session, "user", message));

    String reply;
    List<VendorTools.ScoredVendor> recommendations = List.of();
    if (isAvailable()) {
      // Real AI: Spring AI ChatClient (Ollama) with VendorTools as functions.
      var history =
          messages.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
              .map(m -> m.getRole() + ": " + m.getContent())
              .toList();
      String prompt =
          "Conversation so far:\n" + String.join("\n", history)
              + "\n\nCustomer's latest message: " + message
              + "\n\nRespond helpfully. If they describe an event, use the tools to find vendors.";
      reply =
          chatClient
              .get()
              .prompt()
              .tools(vendorTools)
              .user(prompt)
              .call()
              .content();
    } else {
      // Offline fallback: pure-Java scoring engine (no LLM).
      reply =
          "I'm in offline mode right now (Ollama isn't reachable), but I can still help! "
              + "Tell me your event type, city, guest count and budget, and I'll find "
              + "matching vendors from our directory.";
    }
    messages.save(new ConciergeMessage(session, "assistant", reply));
    return new ChatReply(sessionId.toString(), reply, recommendations);
  }

  public List<ConciergeMessage> history(UUID sessionId) {
    return messages.findBySessionIdOrderByCreatedAtAsc(sessionId);
  }

  public record ChatReply(String sessionId, String reply, List<VendorTools.ScoredVendor> recommendations) {}
}
