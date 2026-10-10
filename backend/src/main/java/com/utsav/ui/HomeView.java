package com.utsav.ui;

import com.utsav.vendor.VendorDto;
import com.utsav.vendor.VendorService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.math.BigDecimal;
import org.springframework.data.domain.PageRequest;

/** Home: vendor discovery with search filters. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Utsav — Discover vendors")
public class HomeView extends VerticalLayout {

  private final VendorService vendorService;

  public HomeView(VendorService vendorService) {
    this.vendorService = vendorService;
    addClassName("utsav-view");
    setSpacing(true);

    H2 heading = new H2("Find vendors for your celebration");
    heading.addClassName("utsav-h2");
    Paragraph sub = new Paragraph(
        "Photographers, decorators, mehendi artists, caterers — verified and budget-aware.");
    sub.addClassName("utsav-sub");

    // Filters
    ComboBox<Category> category = new ComboBox<>("Category");
    category.setItems(Category.values());
    category.setItemLabelGenerator(Category::label);
    category.setClearButtonVisible(true);

    TextField city = new TextField("City");
    city.setPlaceholder("e.g. Boston");
    city.setClearButtonVisible(true);

    NumberField maxPrice = new NumberField("Max budget");
    maxPrice.setMin(0);

    Button search = new Button("Search", e -> doSearch(category, city, maxPrice));
    search.addClassName("utsav-btn-primary");

    HorizontalLayout filters = new HorizontalLayout(category, city, maxPrice, search);
    filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
    filters.addClassName("utsav-filters");

    Div grid = new Div();
    grid.addClassName("utsav-card-grid");

    add(heading, sub, filters, grid);
    doSearch(category, city, maxPrice);
  }

  private void doSearch(ComboBox<Category> category, TextField city, NumberField maxPrice) {
    Div grid = (Div) getComponentAt(3);
    grid.removeAll();
    String cat = category.getValue() == null ? null : category.getValue().slug();
    String cityVal = city.getValue() == null || city.getValue().isBlank() ? null : city.getValue();
    BigDecimal max =
        maxPrice.getValue() == null ? null : BigDecimal.valueOf(maxPrice.getValue());
    vendorService
        .search(cat, cityVal, max, false, PageRequest.of(0, 24))
        .map(VendorDto.View::of)
        .forEach(v -> grid.add(card(v)));
    if (grid.getComponentCount() == 0) {
      grid.add(new Paragraph("No vendors yet — be the first to list, or try the demo seed."));
    }
  }

  private Div card(VendorDto.View v) {
    Div card = new Div();
    card.addClassName("utsav-card");
    card.addClickListener(e -> UI.getCurrent().navigate("vendor-detail/" + v.id()));

    Span name = new Span(v.businessName());
    name.addClassName("utsav-card-title");
    Span meta = new Span(v.categorySlug() + " · " + v.city());
    meta.addClassName("utsav-card-meta");

    Span rating = new Span("★ " + v.ratingAvg() + " (" + v.reviewCount() + ")");
    rating.addClassName("utsav-rating");

    Span price = new Span(
        v.basePrice() != null ? "$" + v.basePrice() + " / " + v.priceUnit() : "Price on request");
    price.addClassName("utsav-price");

    if ("VERIFIED".equals(v.verifiedStatus())) {
      Span badge = new Span("✓ Verified");
      badge.addClassName("utsav-badge-verified");
      card.add(badge);
    }
    card.add(name, meta, rating, price);
    return card;
  }

  enum Category {
    PHOTOGRAPHERS("photographers", "Photographers"),
    DECORATORS("decorators", "Decorators"),
    CATERERS("caterers", "Caterers"),
    MEHENDI("mehendi-artists", "Mehendi Artists"),
    MAKEUP("makeup-artists", "Makeup Artists"),
    DJS("djs", "DJs & Music"),
    VENUES("venues", "Venues"),
    PLANNERS("planners", "Planners");

    private final String slug;
    private final String label;

    Category(String slug, String label) {
      this.slug = slug;
      this.label = label;
    }

    String slug() { return slug; }
    String label() { return label; }
  }
}
