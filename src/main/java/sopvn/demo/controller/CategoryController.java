package sopvn.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import sopvn.demo.entity.Category;
import sopvn.demo.entity.Product;
import sopvn.demo.repository.CategoryRepository;
import sopvn.demo.repository.ColorRepository;
import sopvn.demo.repository.ProductRepository;
import sopvn.demo.repository.SizeRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Controller triển khai tính năng CL-02: Bộ lọc sản phẩm đa tiêu chí
 * Tuyến đường: /danh-muc/{slug}, /danh-muc, /san-pham, /tim-kiem
 */
@Controller
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;

    public CategoryController(CategoryRepository categoryRepository,
                              ProductRepository productRepository,
                              ColorRepository colorRepository,
                              SizeRepository sizeRepository) {
        this.categoryRepository = categoryRepository;
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
                               @RequestParam(value = "sort", required = false, defaultValue = "best-seller") String sort,
                               Model model) {
        Category category = categoryRepository.findBySlug(slug).orElse(null);
        Integer categoryId = (category != null) ? category.getId() : null;
        String currentUri = "/danh-muc/" + slug;

        return renderProductList(categoryId, category, null, colorId, sizeId, minPrice, maxPrice, null, sort, currentUri, model);
    }

    @GetMapping("/san-pham")
    public String allProducts(@RequestParam(value = "colorId", required = false) Integer colorId,
                              @RequestParam(value = "sizeId", required = false) Integer sizeId,
                              @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
                              @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                              @RequestParam(value = "sort", required = false, defaultValue = "best-seller") String sort,
                              Model model) {
        String currentUri = "/san-pham";
        return renderProductList(null, null, null, colorId, sizeId, minPrice, maxPrice, null, sort, currentUri, model);
    }

    @GetMapping("/tim-kiem")
    public String searchProducts(@RequestParam(value = "q", required = false) String keyword,
                                 @RequestParam(value = "colorId", required = false) Integer colorId,
                                 @RequestParam(value = "sizeId", required = false) Integer sizeId,
                                 @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
                                 @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                                 @RequestParam(value = "sort", required = false, defaultValue = "best-seller") String sort,
                                 Model model) {
        String currentUri = "/tim-kiem";
        return renderProductList(null, null, null, colorId, sizeId, minPrice, maxPrice, keyword, sort, currentUri, model);
    }

    private String renderProductList(Integer categoryId,
                                     Category category,
                                     Integer collectionId,
                                     Integer colorId,
                                     Integer sizeId,
                                     BigDecimal minPrice,
                                     BigDecimal maxPrice,
                                     String keyword,
                                     String sort,
                                     String currentUri,
                                     Model model) {
        List<Product> products = productRepository.filterProducts(categoryId, collectionId, colorId, sizeId, minPrice, maxPrice, keyword);

        // Sort products
        if ("price-asc".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparing(Product::getBasePrice));
        } else if ("price-desc".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparing(Product::getBasePrice).reversed());
        } else if ("newest".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparing(Product::getId).reversed());
        } else if ("rating".equalsIgnoreCase(sort)) {
            products.sort(Comparator.comparing(Product::getRatingAvg, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
        } else { // default: best-seller
            products.sort(Comparator.comparing(Product::getSoldCount, Comparator.nullsLast(Integer::compareTo)).reversed());
        }

        model.addAttribute("category", category);
        model.addAttribute("products", products);
        model.addAttribute("totalProducts", products.size());
        model.addAttribute("rootCategories", categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("colors", colorRepository.findAll());
        model.addAttribute("sizes", sizeRepository.findAllByOrderByDisplayOrderAsc());

        // Retain filter parameters
        model.addAttribute("selectedColorId", colorId);
        model.addAttribute("selectedSizeId", sizeId);
        model.addAttribute("selectedMinPrice", minPrice);
        model.addAttribute("selectedMaxPrice", maxPrice);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentUri", currentUri);

        return "category";
    }
}
