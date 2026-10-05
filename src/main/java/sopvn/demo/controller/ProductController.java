package sopvn.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import sopvn.demo.entity.Color;
import sopvn.demo.entity.Product;
import sopvn.demo.entity.ProductImage;
import sopvn.demo.entity.ProductVariant;
import sopvn.demo.entity.Size;
import sopvn.demo.repository.ProductRepository;
import sopvn.demo.repository.ProductVariantRepository;

import java.util.*;

@Controller
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductController(ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @GetMapping("/san-pham/{slug}")
    public String productDetail(@PathVariable("slug") String slug, Model model) {
        Product product = productRepository.findBySlug(slug).orElse(null);
        if (product == null) {
            return "redirect:/san-pham";
        }

        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrue(product.getId());

        List<Map<String, Object>> variantData = new ArrayList<>();
        Set<Color> distinctColors = new LinkedHashSet<>();
        Set<Size> distinctSizes = new LinkedHashSet<>();

        for (ProductVariant v : variants) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("sku", v.getSku());
            map.put("originalPrice", v.getOriginalPrice());
            map.put("salePrice", v.getSalePrice());
            map.put("stockQuantity", v.getStockQuantity());
            if (v.getColor() != null) {
                distinctColors.add(v.getColor());
                Map<String, Object> c = new HashMap<>();
                c.put("id", v.getColor().getId());
                c.put("name", v.getColor().getName());
                c.put("hexCode", v.getColor().getHexCode());
                map.put("color", c);
            }
            if (v.getSize() != null) {
                distinctSizes.add(v.getSize());
                Map<String, Object> s = new HashMap<>();
                s.put("id", v.getSize().getId());
                s.put("name", v.getSize().getName());
                map.put("size", s);
            }
            variantData.add(map);
        }

        Map<Integer, String> colorImageMap = new HashMap<>();
        if (product.getImages() != null) {
            for (ProductImage img : product.getImages()) {
                if (img.getColor() != null && !colorImageMap.containsKey(img.getColor().getId())) {
                    colorImageMap.put(img.getColor().getId(), img.getImageUrl());
                }
            }
        }

        model.addAttribute("product", product);
        model.addAttribute("variants", variants);
        model.addAttribute("variantData", variantData);
        model.addAttribute("distinctColors", distinctColors);
        model.addAttribute("distinctSizes", distinctSizes);
        model.addAttribute("colorImageMap", colorImageMap);
        return "product-detail";
    }
}
