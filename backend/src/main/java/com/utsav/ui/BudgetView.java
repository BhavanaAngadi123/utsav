package com.utsav.ui;

import com.utsav.budget.BudgetService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.math.BigDecimal;

/** Budget Freeze: lock guest count + max budget. */
@Route(value = "budget", layout = MainLayout.class)
@PageTitle("Utsav — Budget Freeze")
public class BudgetView extends VerticalLayout {

  public BudgetView(BudgetService budgetService, SessionContext session) {
    addClassName("utsav-view");
    addClassName("utsav-narrow");
    setSpacing(true);

    H2 heading = new H2("Budget Freeze");
    heading.addClassName("utsav-h2");
    Paragraph sub = new Paragraph(
        "Lock your guest count and max budget. Vendor discovery will only show vendors that fit.");
    sub.addClassName("utsav-sub");

    Div status = new Div();
    status.addClassName("utsav-status");

    NumberField guests = new NumberField("Guest count");
    guests.setMin(1);
    guests.setWidthFull();
    NumberField maxBudget = new NumberField("Max budget (USD)");
    maxBudget.setMin(0);
    maxBudget.setWidthFull();

    Button freeze = new Button("Freeze my budget", e -> {
      if (!session.isLoggedIn()) {
        Notification.show("Please sign in first.");
        return;
      }
      try {
        int g = guests.getValue() == null ? 0 : guests.getValue().intValue();
        BigDecimal max = maxBudget.getValue() == null
            ? null : BigDecimal.valueOf(maxBudget.getValue());
        var f = budgetService.freeze(session.requireUserId(), g, max, "USD");
        Notification.show("Budget frozen: " + g + " guests, $" + max);
        refreshStatus(status, budgetService, session);
      } catch (Exception ex) {
        Notification.show("Couldn't freeze: " + ex.getMessage());
      }
    });
    freeze.addClassName("utsav-btn-primary");
    freeze.setWidthFull();

    add(heading, sub, status, guests, maxBudget, freeze);
    refreshStatus(status, budgetService, session);
  }

  private void refreshStatus(Div status, BudgetService budgetService, SessionContext session) {
    status.removeAll();
    if (!session.isLoggedIn()) {
      status.add(new Span("Sign in to freeze your budget."));
      return;
    }
    budgetService.activeFreeze(session.getUserId()).ifPresentOrElse(
        f -> {
          Span s = new Span(
              "Active freeze: " + f.getGuestCount() + " guests · $" + f.getMaxBudget()
                  + " " + f.getCurrency());
          s.addClassName("utsav-badge-active");
          status.add(s);
        },
        () -> status.add(new Span("No active freeze.")));
  }
}
