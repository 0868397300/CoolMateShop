package sopvn.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import sopvn.demo.entity.Category;
import sopvn.demo.entity.Collection;
import sopvn.demo.entity.Product;
import sopvn.demo.repository.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller triển khai tính năng CL-02: Bộ lọc sản phẩm đa tiêu chí
 * Tuyến đường: /danh-muc/{slug}, /danh-muc, /san-pham, /bo-suu-tap/{slug}, /tim-kiem
 */
@Controller
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;
    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;

    public CategoryController(CategoryRepository categoryRepository,
                              CollectionRepository collectionRepository,
                              ProductRepository productRepository,
                              ColorRepository colorRepository,
                              SizeRepository sizeRepository) {
        this.categoryRepository = categoryRepository;
        this.collectionRepository = collectionRepository;
        this.productRepository = productRepository;
        this.colorRepository = colorRepository;
        this.sizeRepository = sizeRepository;
    }

    @GetMapping("/danh-muc")
    public String categoryRoot() {
        return "redirect:/san-pham";
    }

    @GetMapping("/danh-muc/{slug}")
    public String viewCategory(@PathVariable("slug") String slug,
                               @RequestParam(value = "colorId", required = false) Integer colorId,
                               @RequestParam(value = "sizeId", required = false) Integer sizeId,
                               @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
                               @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                               @RequestParam(value = "material", required = false) String material,
                               @RequestParam(value = "fit", required = false) String fitType,
                               @RequestParam(value = "sort", required = false, defaultValue = "best-seller") String sort,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "12") int size,
                               Model model) {
        Category category = categoryRepository.findBySlug(slug).orElse(null);
        Integer categoryId = (category != null) ? category.getId() : null;
        String currentUri = "/danh-muc/" + slug;

        return renderProductList(categoryId, category, null, null, colorId, sizeId, minPrice, maxPrice, material, fitType, null, sort, page, size, currentUri, model);
    }

    @GetMapping("/bo-suu-tap/{slug}")
    public String viewCollection(@PathVariable("slug") String slug,
                                 @RequestParam(value = "colorId", required = false) Integer colorId,
                                 @RequestParam(value = "sizeId", required = false) Integer sizeId,
                                 @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
                                 @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                                 @RequestParam(value = "material", required = false) String material,
                                 @RequestParam(value = "fit", required = false) String fitType,
                                 @RequestParam(value = "sort", required = false, defaultValue = "best-seller") String sort,
                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "12") int size,
                                 Model model) {
        Collection collection = collectionRepository.findBySlug(slug).orElse(null);
        Integer collectionId = (collection != null) ? collection.getId() : null;
        String currentUri = "/bo-suu-tap/" + slug;

        return renderProductList(null, null, collectionId, collection, colorId, sizeId, minPrice, maxPrice, material, fitType, null, sort, page, size, currentUri, model);
    }

    @GetMapping("/san-pham")
    public String allProducts(@RequestParam(value = "colorId", required = false) Integer colorId,
                              @RequestParam(value = "sizeId", required = false) Integer sizeId,
                              @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
                              @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                              @RequestParam(value = "material", required = false) String material,
                              @RequestParam(value = "fit", required = false) String fitType,
                              @RequestParam(value = "sort", required = false, defaultValue = "best-seller") String sort,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "size", defaultValue = "12") int size,
                              Model model) {
        String currentUri = "/san-pham";
        return renderProductList(null, null, null, null, colorId, sizeId, minPrice, maxPrice, material, fitType, null, sort, page, size, currentUri, model);
    }

    @GetMapping("/tim-kiem")
    public String searchProducts(@RequestParam(value = "q", required = false) String keyword,
                                 @RequestParam(value = "colorId", required = false) Integer colorId,
                                 @RequestParam(value = "sizeId", required = false) Integer sizeId,
                                 @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
                                 @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                                 @RequestParam(value = "material", required = false) String material,
                                 @RequestParam(value = "fit", required = false) String fitType,
                                 @RequestParam(value = "sort", required = false, defaultValue = "best-seller") String sort,
                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "12") int size,
                                 Model model) {
        String currentUri = "/tim-kiem";
        return renderProductList(null, null, null, null, colorId, sizeId, minPrice, maxPrice, material, fitType, keyword, sort, page, size, currentUri, model);
    }

    private String renderProductList(Integer categoryId,
                                     Category category,
                                     Integer collectionId,
                                     Collection collection,
                                     Integer colorId,
                                     Integer sizeId,
                                     BigDecimal minPrice,
                                     BigDecimal maxPrice,
                                     String material,
                                     String fitType,
                                     String keyword,
                                     String sort,
                                     int page,
                                     int size,
                                     String currentUri,
                                     Model model) {
        List<Product> products = productRepository.filterProducts(categoryId, collectionId, colorId, sizeId, minPrice, maxPrice, keyword);

        // Lọc theo Material và FitType nếu có
        if (material != null && !material.isBlank()) {
            String mKw = material.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getMaterial() != null && p.getMaterial().toLowerCase().contains(mKw))
                    .collect(Collectors.toList());
        }

        if (fitType != null && !fitType.isBlank()) {
            String fKw = fitType.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getFitType() != null && p.getFitType().toLowerCase().contains(fKw))
                    .collect(Collectors.toList());
        }

        // Sort products: newest dựa trên ngày tạo (createdAt)
        if ("price-asc".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparing(Product::getBasePrice, Comparator.nullsLast(BigDecimal::compareTo)));
        } else if ("price-desc".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparing(Product::getBasePrice, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
        } else if ("newest".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        } else if ("rating".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparing(Product::getRatingAvg, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
        } else { // default: best-seller
            products.sort(Comparator.comparing(Product::getSoldCount, Comparator.nullsLast(Integer::compareTo)).reversed());
        }

        // Phân trang (Pagination)
        int totalItems = products.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / size));
        int validPage = Math.max(1, Math.min(page, totalPages));
        int start = Math.min((validPage - 1) * size, totalItems);
        int end = Math.min(start + size, totalItems);
        List<Product> pagedProducts = (start < totalItems) ? products.subList(start, end) : Collections.emptyList();

        model.addAttribute("category", category);
        model.addAttribute("collection", collection);
        model.addAttribute("products", pagedProducts);
        model.addAttribute("totalProducts", totalItems);
        model.addAttribute("currentPage", validPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);

        model.addAttribute("rootCategories", categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("collections", collectionRepository.findByIsActiveTrue());
        model.addAttribute("colors", colorRepository.findAll());
        model.addAttribute("sizes", sizeRepository.findAllByOrderByDisplayOrderAsc());

        // Retain filter parameters
        model.addAttribute("selectedColorId", colorId);
        model.addAttribute("selectedSizeId", sizeId);
        model.addAttribute("selectedMinPrice", minPrice);
        model.addAttribute("selectedMaxPrice", maxPrice);
        model.addAttribute("selectedMaterial", material);
        model.addAttribute("selectedFit", fitType);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentUri", currentUri);

        return "category";
    }
}
