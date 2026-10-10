package com.utsav.ui;

import com.utsav.concierge.ConciergeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.UUID;

/** AI concierge chat: describe your event, get vendor recommendations. */
@Route(value = "concierge", layout = MainLayout.class)
@PageTitle("Utsav — AI Concierge")
public class ConciergeView extends VerticalLayout {

  private final ConciergeService conciergeService;
  private final SessionContext session;
  private UUID sessionId;
  private final Div messages = new Div();

  public ConciergeView(ConciergeService conciergeService, SessionContext session) {
    this.conciergeService = conciergeService;
    this.session = session;
    addClassName("utsav-view");
    setSpacing(true);

    H2 heading = new H2("AI Concierge");
    heading.addClassName("utsav-h2");
    Paragraph sub = new Paragraph(
        "Describe your event in plain words — I'll find the best vendors for you.");
    sub.addClassName("utsav-sub");

    if (!conciergeService.isAvailable()) {
      Paragraph offline = new Paragraph(
          "AI is in offline mode (Ollama not reachable). The Java recommendation engine still works.");
      offline.addClassName("utsav-notice");
      add(offline);
    }

    messages.addClassName("utsav-chat");
    messages.setWidthFull();

    TextField input = new TextField();
    input.setPlaceholder("e.g. small outdoor mehendi, 100 guests, budget $5000, near Boston");
    input.setWidthFull();
    Button send = new Button("Send", e -> send(input));
    send.addClassName("utsav-btn-primary");
    input.addKeyPressListener(
        com.vaadin.flow.component.Key.ENTER, e -> send(input));

    com.vaadin.flow.component.orderedlayout.HorizontalLayout row =
        new com.vaadin.flow.component.orderedlayout.HorizontalLayout(input, send);
    row.setWidthFull();
    row.setFlexGrow(1, input);

    add(heading, sub, messages, row);
    startSession();
  }

  private void startSession() {
    UUID customerId = session.isLoggedIn() ? session.getUserId() : null;
    sessionId = conciergeService.startSession(customerId).getId();
    addMessage("assistant",
        "Hi! I'm your Utsav concierge. Tell me about your event — "
            + "what's the occasion, how many guests, your budget, and where?");
  }

  private void send(TextField input) {
    String text = input.getValue();
    if (text == null || text.isBlank()) return;
    input.clear();
    addMessage("user", text);
    try {
      UUID customerId = session.isLoggedIn() ? session.getUserId() : null;
      var reply = conciergeService.chat(sessionId, customerId, text);
      addMessage("assistant", reply.reply());
      if (reply.recommendations() != null) {
        for (var rec : reply.recommendations()) {
          Div card = new Div();
          card.addClassName("utsav-rec-card");
          card.add(
              new Span(rec.vendor().businessName() + " ★ " + rec.vendor().ratingAvg()),
              new Paragraph(rec.reasons()));
          messages.add(card);
        }
      }
    } catch (Exception e) {
      addMessage("assistant", "Sorry, something went wrong: " + e.getMessage());
    }
  }

  private void addMessage(String role, String text) {
    Div bubble = new Div();
    bubble.addClassName("utsav-msg-" + role);
    bubble.setText(text);
    messages.add(bubble);
    bubble.getElement().callJsFunction("$0.scrollIntoView", bubble);
  }
}
