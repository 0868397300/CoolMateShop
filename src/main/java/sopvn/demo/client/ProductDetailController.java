package sopvn.demo.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/client")
public class ProductDetailController {

    @GetMapping("/san-pham/{slug}")
    public String detail(@PathVariable("slug") String slug) {
        return "redirect:/san-pham/" + slug;
    }
}
