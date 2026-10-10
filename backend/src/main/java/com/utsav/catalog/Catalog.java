package com.utsav.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vendor categories catalog. */
@Entity
@Table(name = "categories")
class Category {
  @Id
  private String slug;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected Category() {}

  public String getSlug() { return slug; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public int getSortOrder() { return sortOrder; }
}

interface CategoryRepository extends JpaRepository<Category, String> {
  List<Category> findAllByOrderBySortOrderAsc();
}

@Service
class CatalogService {
  private final CategoryRepository categories;

  CatalogService(CategoryRepository categories) {
    this.categories = categories;
  }

  List<Category> all() {
    return categories.findAllByOrderBySortOrderAsc();
  }
}

@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Catalog")
class CatalogController {

  private final CatalogService catalogService;

  CatalogController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/categories")
  @Operation(summary = "Vendor categories")
  List<CategoryView> categories() {
    return catalogService.all().stream()
        .map(c -> new CategoryView(c.getSlug(), c.getName(), c.getDescription()))
        .toList();
  }

  record CategoryView(String slug, String name, String description) {}
}
