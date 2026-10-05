package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import sopvn.demo.repository.CategoryRepository;
import sopvn.demo.repository.ProductRepository;

@Controller
@RequestMapping("/admin/san-pham")
public class AdminProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public AdminProductController(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "admin/product-list";
    }

    @GetMapping("/them")
    public String addProductForm(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product-form";
    }
}
