package com.utsav.ui;

import com.utsav.booking.BookingService;
import com.utsav.vendor.Vendor;
import com.utsav.vendor.VendorDto;
import com.utsav.vendor.VendorService;
import com.utsav.verification.IdVerification;
import com.utsav.verification.VerificationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;

/** Vendor dashboard: my profiles, bookings, ID verification, slots. */
@Route(value = "vendor", layout = MainLayout.class)
@PageTitle("Utsav — Vendor Dashboard")
public class VendorDashboardView extends VerticalLayout {

  public VendorDashboardView(
      VendorService vendorService,
      BookingService bookingService,
      VerificationService verificationService,
      SessionContext session) {
    addClassName("utsav-view");
    setSpacing(true);

    H2 title = new H2("Vendor Dashboard");
    title.addClassName("utsav-h2");
    add(title);

    if (!session.isLoggedIn()) {
      add(new Paragraph("Please sign in as a vendor."));
      return;
    }

    List<Vendor> profiles = vendorService.myProfiles(session.requireUserId());
    if (profiles.isEmpty()) {
      add(new Paragraph("No vendor profile yet. Create one:"));
      TextField business = new TextField("Business name");
      ComboBox<String> category = new ComboBox<>("Category",
          "photographers", "decorators", "caterers", "mehendi-artists",
          "makeup-artists", "djs", "venues", "planners");
      TextField city = new TextField("City");
      TextField country = new TextField("Country code");
      country.setValue("US");
      Button create = new Button("Create profile", e -> {});
      create.addClassName("utsav-btn-primary");
      create.addClickListener(
          e -> {
            try {
              Vendor draft =
                  VendorDto.toEntity(
                      new VendorDto.Upsert(
                          business.getValue(),
                          category.getValue(),
                          null,
                          city.getValue(),
                          country.getValue(),
                          "USD",
                          null,
                          "event",
                          null,
                          null,
                          null,
                          true));
              vendorService.createProfile(session.requireUserId(), draft);
              Notification.show("Profile created — refresh the page.");
            } catch (RuntimeException ex) {
              Notification.show("Couldn't create: " + ex.getMessage());
            }
          });
      add(new HorizontalLayout(business, category, city, country, create));
      return;
    }

    for (Vendor v : profiles) {
      Div card = new Div();
      card.addClassName("utsav-vendor-card");
      card.add(new H2(v.getBusinessName()));

      // Verification status
      Paragraph ver = new Paragraph("Verification: " + v.getVerifiedStatus());
      card.add(ver);
      if (v.getVerifiedStatus() != Vendor.VerificationStatus.VERIFIED) {
        ComboBox<String> countryPick = new ComboBox<>("Country",
            VerificationService.COUNTRY_ID_TYPES.keySet().stream().sorted().toList());
        ComboBox<String> idType = new ComboBox<>("ID type");
        countryPick.addValueChangeListener(e -> {
          if (e.getValue() != null) {
            idType.setItems(VerificationService.COUNTRY_ID_TYPES.get(e.getValue()));
          }
        });
        Button submit = new Button("Submit verification", e -> {
          try {
            verificationService.submit(v.getId(), session.requireUserId(),
                countryPick.getValue(), idType.getValue());
            Notification.show("Verification submitted!");
          } catch (Exception ex) {
            Notification.show("Couldn't submit: " + ex.getMessage());
          }
        });
        card.add(new HorizontalLayout(countryPick, idType, submit));
      }

      // Bookings
      Grid<BookingRow> grid = new Grid<>(BookingRow.class, false);
      grid.addColumn(BookingRow::date).setHeader("Date");
      grid.addColumn(BookingRow::status).setHeader("Status");
      grid.setItems(bookingService.vendorBookings(v.getId()).stream()
          .map(b -> new BookingRow(
              b.getEventDate() == null ? "" : b.getEventDate().toString(),
              b.getStatus().name()))
          .toList());
      card.add(grid);

      // Block a slot
      DatePicker date = new DatePicker("Block date");
      TextField label = new TextField("Slot label");
      label.setPlaceholder("e.g. Morning");
      Button block = new Button("Block slot", e -> {
        try {
          bookingService.blockSlot(v.getId(), date.getValue(), label.getValue());
          Notification.show("Slot blocked.");
        } catch (Exception ex) {
          Notification.show("Couldn't block: " + ex.getMessage());
        }
      });
      card.add(new HorizontalLayout(date, label, block));

      add(card);
    }
  }

  record BookingRow(String date, String status) {}
}
