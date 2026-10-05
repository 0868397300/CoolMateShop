package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.entity.CoolCashTransaction;
import sopvn.demo.entity.User;
import sopvn.demo.repository.CoolCashTransactionRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/khach-hang")
public class AdminCustomerController {

    private final UserRepository userRepository;
    private final CoolCashTransactionRepository coolCashTransactionRepository;

    public AdminCustomerController(UserRepository userRepository,
                                   CoolCashTransactionRepository coolCashTransactionRepository) {
        this.userRepository = userRepository;
        this.coolCashTransactionRepository = coolCashTransactionRepository;
    }

    @GetMapping
    public String listCustomers(@RequestParam(value = "keyword", required = false) String keyword,
                                @RequestParam(value = "tier", required = false) String tier,
                                @RequestParam(value = "status", required = false) String status,
                                Model model) {
        List<User> customers = userRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase().trim();
            customers = customers.stream()
                    .filter(c -> (c.getFullName() != null && c.getFullName().toLowerCase().contains(kw)) ||
                                 (c.getEmail() != null && c.getEmail().toLowerCase().contains(kw)) ||
                                 (c.getPhone() != null && c.getPhone().contains(kw)))
                    .collect(Collectors.toList());
        }

        if (tier != null && !tier.isBlank()) {
            customers = customers.stream()
                    .filter(c -> tier.equalsIgnoreCase(c.getMembershipTier()))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            boolean active = "ACTIVE".equalsIgnoreCase(status);
            customers = customers.stream()
                    .filter(c -> Boolean.valueOf(active).equals(c.getIsActive()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("customers", customers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("tier", tier);
        model.addAttribute("status", status);
        return "admin/customer-list";
    }

    @PostMapping("/{userId}/dieu-chinh-vi")
    public String adjustCoolCash(@PathVariable("userId") Long userId,
                                 @RequestParam("amount") BigDecimal amount,
                                 @RequestParam("type") String type, // PLUS or MINUS
                                 @RequestParam("reason") String reason,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal actualAmount = "MINUS".equalsIgnoreCase(type) ? amount.negate() : amount;
            BigDecimal current = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;
            BigDecimal newBalance = current.add(actualAmount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;
            user.setCoolcashBalance(newBalance);
            userRepository.save(user);

            // Ghi lịch sử giao dịch
            CoolCashTransaction tx = new CoolCashTransaction();
            tx.setUser(user);
            tx.setAmount(actualAmount);
            tx.setTransactionType("ADMIN_ADJUST");
            tx.setStatus("COMPLETED");
            tx.setDescription("Admin điều chỉnh ví: " + reason);
            tx.setCreatedAt(LocalDateTime.now());
            coolCashTransactionRepository.save(tx);

            redirectAttributes.addFlashAttribute("successMessage", "Điều chỉnh số dư ví CoolCash cho " + user.getFullName() + " thành công!");
        }
        return "redirect:/admin/khach-hang";
    }

    @PostMapping("/{userId}/doi-hang")
    public String changeTier(@PathVariable("userId") Long userId,
                             @RequestParam("tier") String tier,
                             RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setMembershipTier(tier.toUpperCase());
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hạng hội viên " + tier + " cho " + user.getFullName() + " thành công!");
        }
        return "redirect:/admin/khach-hang";
    }

    @PostMapping("/{userId}/toggle-status")
    public String toggleStatus(@PathVariable("userId") Long userId, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            boolean newStatus = !Boolean.TRUE.equals(user.getIsActive());
            user.setIsActive(newStatus);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", (newStatus ? "Đã mở khóa tài khoản " : "Đã khóa tài khoản ") + user.getFullName());
        }
        return "redirect:/admin/khach-hang";
    }
}