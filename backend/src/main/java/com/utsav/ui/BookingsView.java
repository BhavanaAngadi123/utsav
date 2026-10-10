package com.utsav.ui;

import com.utsav.booking.BookingController;
import com.utsav.booking.BookingService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** My bookings (customer). */
@Route(value = "bookings", layout = MainLayout.class)
@PageTitle("Utsav — My Bookings")
public class BookingsView extends VerticalLayout {

  public BookingsView(BookingService bookingService, SessionContext session) {
    addClassName("utsav-view");
    setSpacing(true);

    H2 title = new H2("My Bookings");
    title.addClassName("utsav-h2");

    if (!session.isLoggedIn()) {
      add(title, new Paragraph("Please sign in to see your bookings."));
      return;
    }

    Grid<BookingController.BookingView> grid = new Grid<>(BookingController.BookingView.class, false);
    grid.addColumn(v -> v.vendorName()).setHeader("Vendor");
    grid.addColumn(v -> v.eventDate() == null ? "" : v.eventDate().toString()).setHeader("Date");
    grid.addColumn(v -> v.slotLabel()).setHeader("Slot");
    grid.addColumn(v -> v.status()).setHeader("Status");
    grid.addColumn(v -> v.paymentStatus()).setHeader("Payment");
    grid.setItems(
        bookingService.myBookings(session.requireUserId()).stream()
            .map(BookingController.BookingView::of)
            .toList());
    add(title, grid);
  }
}
