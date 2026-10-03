package sopvn.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sopvn.demo.entity.Category;
import sopvn.demo.entity.Product;
import sopvn.demo.repository.CategoryRepository;
import sopvn.demo.repository.ProductRepository;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/")
    public String home(Model model) {
        List<Category> categories = categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();
        List<Product> bestSellers = productRepository.findTop8ByStatusOrderBySoldCountDesc("ACTIVE");

        model.addAttribute("categories", categories);
        model.addAttribute("bestSellers", bestSellers);

        return "index";
    }
}
