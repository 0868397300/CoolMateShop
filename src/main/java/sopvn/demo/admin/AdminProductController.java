package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.core.util.SlugUtils;
import sopvn.demo.entity.*;
import sopvn.demo.inventory.StockService;
import java.security.Principal;
import sopvn.demo.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/san-pham")
public class AdminProductController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final StockService stockService;
    private final UserRepository userRepository;

    public AdminProductController(ProductRepository productRepository,
                                  ProductVariantRepository productVariantRepository,
                                  ProductImageRepository productImageRepository,
                                  CategoryRepository categoryRepository,
                                  CollectionRepository collectionRepository,
                                  ColorRepository colorRepository,
                                  SizeRepository sizeRepository,
                                  StockService stockService,
                                  UserRepository userRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.collectionRepository = collectionRepository;
        this.colorRepository = colorRepository;
        this.sizeRepository = sizeRepository;
        this.stockService = stockService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listProducts(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "categoryId", required = false) Integer categoryId,
                               @RequestParam(value = "status", required = false) String status,
                               Model model) {
        List<Product> products = productRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(kw))
                            || (p.getSlug() != null && p.getSlug().toLowerCase().contains(kw))
                            || (p.getMaterial() != null && p.getMaterial().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && categoryId.equals(p.getCategory().getId()))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            products = products.stream()
                    .filter(p -> status.equalsIgnoreCase(p.getStatus()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStatus", status);

        return "admin/product-list";
    }

    @GetMapping("/them")
    public String addProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("collections", collectionRepository.findAll());
        model.addAttribute("colors", colorRepository.findAll());
        model.addAttribute("sizes", sizeRepository.findAllByOrderByDisplayOrderAsc());
        return "admin/product-form";
    }

    @PostMapping("/them")
    public String saveProduct(@RequestParam("name") String name,
                              @RequestParam("categoryId") Integer categoryId,
                              @RequestParam(value = "collectionId", required = false) Integer collectionId,
                              @RequestParam("basePrice") BigDecimal basePrice,
                              @RequestParam(value = "material", required = false) String material,
                              @RequestParam(value = "fitType", required = false) String fitType,
                              @RequestParam(value = "shortDescription", required = false) String shortDescription,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "features", required = false) String features,
                              @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
                              @RequestParam(value = "imageUrl", required = false) String imageUrl,
                              // Biến thể khởi tạo
                              @RequestParam(value = "colorId", required = false) Integer colorId,
                              @RequestParam(value = "sizeId", required = false) Integer sizeId,
                              @RequestParam(value = "sku", required = false) String sku,
                              @RequestParam(value = "salePrice", required = false) BigDecimal salePrice,
                              @RequestParam(value = "stockQuantity", required = false) Integer stockQuantity,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            if (name == null || name.isBlank()) {
                throw new CustomException("Tên sản phẩm không được để trống.");
            }
            if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new CustomException("Giá cơ bản sản phẩm không hợp lệ.");
            }

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CustomException("Danh mục không tồn tại: " + categoryId));

            Collection collection = (collectionId != null) ? collectionRepository.findById(collectionId).orElse(null) : null;

            Product p = new Product();
            p.setName(name.trim());
            String slug = SlugUtils.toSlug(name);
            if (productRepository.findBySlug(slug).isPresent()) {
                slug = slug + "-" + System.currentTimeMillis();
            }
            p.setSlug(slug);
            p.setCategory(category);
            p.setCollection(collection);
            p.setBasePrice(basePrice);
            p.setMaterial(material);
            p.setFitType(fitType);
            p.setShortDescription(shortDescription);
            p.setDescription(description);
            p.setFeatures(features);
            p.setStatus(status != null ? status.toUpperCase() : "ACTIVE");
            p.setRatingAvg(BigDecimal.ZERO);
            p.setReviewCount(0);
            p.setSoldCount(0);

            Product savedProduct = productRepository.save(p);

            // Thêm ảnh nếu có
            if (imageUrl != null && !imageUrl.isBlank()) {
                ProductImage img = new ProductImage(savedProduct, null, imageUrl.trim(), true, 0);
                productImageRepository.save(img);
            }

            // Thêm biến thể ban đầu nếu được cung cấp
            if (colorId != null && sizeId != null) {
                Color c = colorRepository.findById(colorId).orElse(null);
                Size s = sizeRepository.findById(sizeId).orElse(null);
                if (c != null && s != null) {
                    String finalSku = (sku != null && !sku.isBlank()) ? sku.trim().toUpperCase() : ("SKU-" + savedProduct.getId() + "-" + c.getId() + "-" + s.getId());
                    if (productVariantRepository.existsBySku(finalSku)) {
                        throw new CustomException("Mã SKU '" + finalSku + "' đã tồn tại trong hệ thống!");
                    }
                    ProductVariant v = new ProductVariant(savedProduct, c, s, finalSku, basePrice, (salePrice != null ? salePrice : basePrice), 0, 250, true);
                    v = productVariantRepository.save(v);
                    if (stockQuantity != null && stockQuantity > 0) {
                        stockService.adjustStockAudited(v, stockQuantity, "Tồn kho ban đầu khi tạo sản phẩm " + savedProduct.getName(), currentUser);
                    }
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới sản phẩm '" + savedProduct.getName() + "' thành công!");
            return "redirect:/admin/san-pham/" + savedProduct.getId() + "/sua";
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/san-pham/them";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
            return "redirect:/admin/san-pham/them";
        }
    }

    @GetMapping("/{id}/sua")
    public String editProductForm(@PathVariable("id") Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomException("Không tìm thấy sản phẩm ID: " + id));

        model.addAttribute("product", product);
        model.addAttribute("variants", productVariantRepository.findByProductId(id));
        model.addAttribute("images", productImageRepository.findByProductIdOrderByDisplayOrderAsc(id));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("collections", collectionRepository.findAll());
        model.addAttribute("colors", colorRepository.findAll());
        model.addAttribute("sizes", sizeRepository.findAllByOrderByDisplayOrderAsc());

        return "admin/product-form";
    }

    @PostMapping("/{id}/sua")
    public String updateProduct(@PathVariable("id") Long id,
                                @RequestParam("name") String name,
                                @RequestParam("categoryId") Integer categoryId,
                                @RequestParam(value = "collectionId", required = false) Integer collectionId,
                                @RequestParam("basePrice") BigDecimal basePrice,
                                @RequestParam(value = "material", required = false) String material,
                                @RequestParam(value = "fitType", required = false) String fitType,
                                @RequestParam(value = "shortDescription", required = false) String shortDescription,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam(value = "features", required = false) String features,
                                @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
                                Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            Product p = productRepository.findById(id)
                    .orElseThrow(() -> new CustomException("Không tìm thấy sản phẩm ID: " + id));

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CustomException("Danh mục không tồn tại: " + categoryId));

            Collection collection = (collectionId != null) ? collectionRepository.findById(collectionId).orElse(null) : null;

            p.setName(name.trim());
            p.setCategory(category);
            p.setCollection(collection);
            p.setBasePrice(basePrice);
            p.setMaterial(material);
            p.setFitType(fitType);
            p.setShortDescription(shortDescription);
            p.setDescription(description);
            p.setFeatures(features);
            p.setStatus(status != null ? status.toUpperCase() : "ACTIVE");

            productRepository.save(p);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin sản phẩm thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }

        return "redirect:/admin/san-pham/" + id + "/sua";
    }

    @PostMapping("/{id}/trang-thai")
    public String toggleStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        productRepository.findById(id).ifPresent(p -> {
            if ("ACTIVE".equalsIgnoreCase(p.getStatus())) {
                p.setStatus("INACTIVE");
            } else {
                p.setStatus("ACTIVE");
            }
            productRepository.save(p);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái sản phẩm: " + p.getName() + " -> " + p.getStatus());
        });
        return "redirect:/admin/san-pham";
    }

    @PostMapping("/{id}/xoa")
    public String deleteProduct(@PathVariable("id") Long id, Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            Product p = productRepository.findById(id).orElse(null);
            if (p != null) {
                p.setStatus("INACTIVE");
                productRepository.save(p);
                redirectAttributes.addFlashAttribute("successMessage", "Đã ngưng kinh doanh sản phẩm: " + p.getName());
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin/san-pham";
    }

    @PostMapping("/{id}/bien-the/them")
    public String addVariant(@PathVariable("id") Long id,
                             @RequestParam("colorId") Integer colorId,
                             @RequestParam("sizeId") Integer sizeId,
                             @RequestParam("sku") String sku,
                             @RequestParam("originalPrice") BigDecimal originalPrice,
                             @RequestParam("salePrice") BigDecimal salePrice,
                             @RequestParam(value = "stockQuantity", defaultValue = "0") Integer stockQuantity,
                             Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new CustomException("Sản phẩm không tồn tại: " + id));

            Color color = colorRepository.findById(colorId)
                    .orElseThrow(() -> new CustomException("Màu sắc không tồn tại: " + colorId));

            Size size = sizeRepository.findById(sizeId)
                    .orElseThrow(() -> new CustomException("Kích cỡ không tồn tại: " + sizeId));

            String finalSku = sku != null ? sku.trim().toUpperCase() : "";
            if (finalSku.isBlank()) {
                throw new CustomException("Mã SKU không được để trống.");
            }

            if (productVariantRepository.existsBySku(finalSku)) {
                throw new CustomException("Mã SKU '" + finalSku + "' đã tồn tại trên hệ thống!");
            }

            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setColor(color);
            variant.setSize(size);
            variant.setSku(finalSku);
            variant.setOriginalPrice(originalPrice != null ? originalPrice : product.getBasePrice());
            variant.setSalePrice(salePrice != null ? salePrice : product.getBasePrice());
            variant.setStockQuantity(0);
            variant.setIsActive(true);
            variant = productVariantRepository.save(variant);

            if (stockQuantity != null && stockQuantity > 0) {
                stockService.adjustStockAudited(variant, stockQuantity, "Tồn kho ban đầu cho biến thể SKU " + finalSku, currentUser);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm biến thể SKU: " + finalSku);
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }

        return "redirect:/admin/san-pham/" + id + "/sua";
    }

    @PostMapping("/{id}/bien-the/{variantId}/xoa")
    public String deleteVariant(@PathVariable("id") Long id,
                                @PathVariable("variantId") Long variantId,
                                Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            productVariantRepository.findById(variantId).ifPresent(v -> {
                v.setIsActive(false);
                productVariantRepository.save(v);
                redirectAttributes.addFlashAttribute("successMessage", "Đã vô hiệu hóa biến thể SKU: " + v.getSku());
            });
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa biến thể: " + ex.getMessage());
        }
        return "redirect:/admin/san-pham/" + id + "/sua";
    }

    @PostMapping("/{id}/anh/them")
    public String addImage(@PathVariable("id") Long id,
                           @RequestParam("imageUrl") String imageUrl,
                           @RequestParam(value = "colorId", required = false) Integer colorId,
                           @RequestParam(value = "isThumbnail", defaultValue = "false") boolean isThumbnail,
                           Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new CustomException("Sản phẩm không tồn tại: " + id));

            Color color = (colorId != null) ? colorRepository.findById(colorId).orElse(null) : null;

            if (isThumbnail) {
                List<ProductImage> existing = productImageRepository.findByProductIdOrderByDisplayOrderAsc(id);
                for (ProductImage img : existing) {
                    if (Boolean.TRUE.equals(img.getIsThumbnail())) {
                        img.setIsThumbnail(false);
                        productImageRepository.save(img);
                    }
                }
            }

            ProductImage img = new ProductImage(product, color, imageUrl.trim(), isThumbnail, 0);
            productImageRepository.save(img);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm ảnh sản phẩm thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin/san-pham/" + id + "/sua";
    }

    @PostMapping("/{id}/anh/{imageId}/xoa")
    public String deleteImage(@PathVariable("id") Long id,
                              @PathVariable("imageId") Long imageId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            productImageRepository.deleteById(imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ảnh sản phẩm.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa ảnh: " + ex.getMessage());
        }
        return "redirect:/admin/san-pham/" + id + "/sua";
    }
}
