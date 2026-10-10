package com.utsav.ui;

import com.utsav.admin.AdminController;
import com.utsav.verification.VerificationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

/** Admin: verification queue, audit log. */
@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Utsav — Admin")
public class AdminView extends VerticalLayout {

  public AdminView(
      VerificationService verificationService,
      com.utsav.admin.AuditService auditService,
      SessionContext session) {
    addClassName("utsav-view");
    setSpacing(true);

    H2 title = new H2("Admin");
    title.addClassName("utsav-h2");
    add(title);

    if (!session.isLoggedIn()) {
      add(new Paragraph("Please sign in as an admin."));
      return;
    }

    // Verification queue
    H2 queueTitle = new H2("Verification queue");
    add(queueTitle);
    Grid<QueueRow> grid = new Grid<>(QueueRow.class, false);
    grid.addColumn(QueueRow::vendorId).setHeader("Vendor");
    grid.addColumn(QueueRow::country).setHeader("Country");
    grid.addColumn(QueueRow::idType).setHeader("ID type");
    grid.addColumn(QueueRow::status).setHeader("Status");
    grid.addComponentColumn(
        row -> {
          Button approve = new Button("Approve", e -> {
            try {
              verificationService.review(
                  UUID.fromString(row.id()), session.requireUserId(), true, "approved via UI");
              Notification.show("Approved");
              refreshQueue(grid, verificationService);
            } catch (Exception ex) {
              Notification.show("Failed: " + ex.getMessage());
            }
          });
          Button reject = new Button("Reject", e -> {
            try {
              verificationService.review(
                  UUID.fromString(row.id()), session.requireUserId(), false, "rejected via UI");
              Notification.show("Rejected");
              refreshQueue(grid, verificationService);
            } catch (Exception ex) {
              Notification.show("Failed: " + ex.getMessage());
            }
          });
          return new com.vaadin.flow.component.orderedlayout.HorizontalLayout(approve, reject);
        })
        .setHeader("Actions");
    add(grid);
    refreshQueue(grid, verificationService);

    // Audit log
    H2 auditTitle = new H2("Audit log");
    add(auditTitle);
    Grid<AuditRow> auditGrid = new Grid<>(AuditRow.class, false);
    auditGrid.addColumn(AuditRow::action).setHeader("Action");
    auditGrid.addColumn(AuditRow::entity).setHeader("Entity");
    auditGrid.addColumn(AuditRow::detail).setHeader("Detail");
    auditGrid.setItems(auditService.recent(PageRequest.of(0, 50)).stream()
        .map(a -> new AuditRow(a.getAction(),
            a.getEntityType() + "/" + a.getEntityId(), a.getDetail()))
        .toList());
    add(auditGrid);
  }

  private void refreshQueue(Grid<QueueRow> grid, VerificationService verificationService) {
    try {
      grid.setItems(verificationService.reviewQueue(PageRequest.of(0, 50)).stream()
          .map(v -> new QueueRow(v.getId().toString(),
              v.getVendor().getId().toString(), v.getCountryCode(), v.getIdType(),
              v.getStatus().name()))
          .toList());
    } catch (Exception e) {
      // Not an admin: leave empty.
    }
  }

  record QueueRow(String id, String vendorId, String country, String idType, String status) {}
  record AuditRow(String action, String entity, String detail) {}
}
