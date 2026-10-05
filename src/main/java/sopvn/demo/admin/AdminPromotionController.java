package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.entity.Category;
import sopvn.demo.entity.ComboRule;
import sopvn.demo.entity.Promotion;
import sopvn.demo.repository.CategoryRepository;
import sopvn.demo.repository.ComboRuleRepository;
import sopvn.demo.repository.PromotionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/khuyen-mai")
public class AdminPromotionController {

    private final PromotionRepository promotionRepository;
    private final ComboRuleRepository comboRuleRepository;
    private final CategoryRepository categoryRepository;

    public AdminPromotionController(PromotionRepository promotionRepository,
                                    ComboRuleRepository comboRuleRepository,
                                    CategoryRepository categoryRepository) {
        this.promotionRepository = promotionRepository;
        this.comboRuleRepository = comboRuleRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String listPromotions(@RequestParam(value = "keyword", required = false) String keyword,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "type", required = false) String type,
                                 Model model) {
        List<Promotion> promotions = promotionRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            promotions = promotions.stream()
                    .filter(p -> (p.getCode() != null && p.getCode().toLowerCase().contains(kw))
                            || (p.getName() != null && p.getName().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            if ("ACTIVE".equalsIgnoreCase(status)) {
                promotions = promotions.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).collect(Collectors.toList());
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                promotions = promotions.stream().filter(p -> !Boolean.TRUE.equals(p.getIsActive())).collect(Collectors.toList());
            }
        }

        if (type != null && !type.isBlank()) {
            promotions = promotions.stream()
                    .filter(p -> type.equalsIgnoreCase(p.getDiscountType()))
                    .collect(Collectors.toList());
        }

        List<ComboRule> comboRules = comboRuleRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        model.addAttribute("promotions", promotions);
        model.addAttribute("comboRules", comboRules);
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);

        return "admin/promotion-list";
    }

    @PostMapping("/tao-moi")
    public String createPromotion(@RequestParam("code") String code,
                                  @RequestParam("name") String name,
                                  @RequestParam("discountType") String discountType,
                                  @RequestParam("discountValue") BigDecimal discountValue,
                                  @RequestParam(value = "maxDiscountAmount", required = false) BigDecimal maxDiscountAmount,
                                  @RequestParam(value = "minOrderValue", required = false) BigDecimal minOrderValue,
                                  @RequestParam(value = "usageLimit", required = false, defaultValue = "100") Integer usageLimit,
                                  RedirectAttributes redirectAttributes) {
        try {
            Promotion p = new Promotion();
            p.setCode(code.trim().toUpperCase());
            p.setName(name.trim());
            p.setDiscountType(discountType);
            p.setDiscountValue(discountValue);
            p.setMaxDiscountAmount(maxDiscountAmount);
            p.setMinOrderValue(minOrderValue != null ? minOrderValue : BigDecimal.ZERO);
            p.setUsageLimit(usageLimit != null ? usageLimit : 100);
            p.setUsedCount(0);
            p.setIsActive(true);

            LocalDateTime now = LocalDateTime.now();
            p.setStartDate(now);
            p.setEndDate(now.plusMonths(6));

            promotionRepository.save(p);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo mã voucher thành công: " + p.getCode());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo mã voucher: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai";
    }

    @PostMapping("/{id}/trang-thai")
    public String togglePromotion(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        promotionRepository.findById(id).ifPresent(p -> {
            p.setIsActive(!Boolean.TRUE.equals(p.getIsActive()));
            promotionRepository.save(p);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái voucher: " + p.getCode());
        });
        return "redirect:/admin/khuyen-mai";
    }

    @PostMapping("/{id}/xoa")
    public String deletePromotion(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa voucher thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa voucher: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai";
    }

    @PostMapping("/combo/tao-moi")
    public String createComboRule(@RequestParam("name") String name,
                                  @RequestParam("categoryId") Integer categoryId,
                                  @RequestParam("minQuantity") Integer minQuantity,
                                  @RequestParam("discountPercentage") BigDecimal discountPercentage,
                                  RedirectAttributes redirectAttributes) {
        try {
            Category cat = categoryRepository.findById(categoryId).orElseThrow();
            ComboRule rule = new ComboRule();
            rule.setName(name.trim());
            rule.setCategory(cat);
            rule.setMinQuantity(minQuantity);
            rule.setDiscountPercentage(discountPercentage);
            rule.setIsActive(true);

            comboRuleRepository.save(rule);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo quy tắc Combo Mix & Match thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo combo rule: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai";
    }

    @PostMapping("/combo/{id}/trang-thai")
    public String toggleComboRule(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        comboRuleRepository.findById(id).ifPresent(rule -> {
            rule.setIsActive(!Boolean.TRUE.equals(rule.getIsActive()));
            comboRuleRepository.save(rule);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái quy tắc combo: " + rule.getName());
        });
        return "redirect:/admin/khuyen-mai";
    }

    @PostMapping("/combo/{id}/xoa")
    public String deleteComboRule(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            comboRuleRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa quy tắc combo thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa combo rule: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai";
    }
}