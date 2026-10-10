package com.utsav.ui;

import com.utsav.booking.BookingService;
import com.utsav.review.ReviewService;
import com.utsav.vendor.VendorDto;
import com.utsav.vendor.VendorService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.UUID;

/** Vendor detail: profile, reviews, book a slot. */
@Route(value = "vendor-detail", layout = MainLayout.class)
@PageTitle("Utsav — Vendor")
public class VendorDetailView extends VerticalLayout implements HasUrlParameter<String> {

  private final VendorService vendorService;
  private final BookingService bookingService;
  private final ReviewService reviewService;
  private final SessionContext session;

  public VendorDetailView(
      VendorService vendorService,
      BookingService bookingService,
      ReviewService reviewService,
      SessionContext session) {
    this.vendorService = vendorService;
    this.bookingService = bookingService;
    this.reviewService = reviewService;
    this.session = session;
    addClassName("utsav-view");
  }

  @Override
  public void setParameter(BeforeEvent event, String parameter) {
    removeAll();
    try {
      UUID id = UUID.fromString(parameter);
      VendorDto.View v = VendorDto.View.of(vendorService.get(id));

      H2 name = new H2(v.businessName());
      name.addClassName("utsav-h2");
      add(name);
      add(new Paragraph(v.description() == null ? "" : v.description()));
      add(new Span(v.categorySlug() + " · " + v.city() + ", " + v.country()));
      add(new Span("★ " + v.ratingAvg() + " (" + v.reviewCount() + " reviews)"));
      if (v.basePrice() != null) {
        add(new Span("$" + v.basePrice() + " / " + v.priceUnit()));
      }

      // Book
      if (session.isLoggedIn()) {
        DatePicker date = new DatePicker("Event date");
        TextField slot = new TextField("Slot");
        slot.setPlaceholder("e.g. Evening");
        Button book = new Button("Request booking", e -> {
          try {
            bookingService.book(session.requireUserId(), id,
                date.getValue(), slot.getValue(), null, null);
            Notification.show("Booking requested!");
          } catch (Exception ex) {
            Notification.show("Couldn't book: " + ex.getMessage());
          }
        });
        book.addClassName("utsav-btn-primary");
        add(new HorizontalLayout(date, slot, book));
      } else {
        add(new Paragraph("Sign in to book this vendor."));
      }

      // Collaborators
      Div collab = new Div();
      collab.add(new H2("Often booked with"));
      vendorService.collaborators(id, 4).stream()
          .map(VendorDto.View::of)
          .forEach(c -> collab.add(new Span(c.businessName() + " · ")));
      add(collab);

      // Reviews
      Div reviews = new Div();
      reviews.add(new H2("Reviews"));
      reviewService.forVendor(id).forEach(r ->
          reviews.add(new Paragraph("★ " + r.getRating() + " — " + r.getComment())));
      add(reviews);

    } catch (Exception e) {
      add(new Paragraph("Vendor not found."));
    }
  }
}
