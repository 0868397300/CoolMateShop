package sopvn.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sopvn.demo.repository.CategoryRepository;
import sopvn.demo.repository.ProductRepository;

@Controller
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public HomeController(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("categories", categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("bestSellers", productRepository.findTop8ByStatusOrderBySoldCountDesc("ACTIVE"));
        return "index";
    }
}
