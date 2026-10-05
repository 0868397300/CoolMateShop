package sopvn.demo.wallet;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sopvn.demo.entity.Promotion;
import sopvn.demo.entity.User;
import sopvn.demo.repository.PromotionRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
public class CoolClubController {

    private final UserRepository userRepository;
    private final PromotionRepository promotionRepository;

    public CoolClubController(UserRepository userRepository, PromotionRepository promotionRepository) {
        this.userRepository = userRepository;
        this.promotionRepository = promotionRepository;
    }

    private User getAuthenticatedUser(Principal principal, HttpSession session) {
        if (principal != null) {
            return userRepository.findByEmail(principal.getName()).orElse(null);
        }
        if (session != null) {
            Object sUser = session.getAttribute("currentUser");
            if (sUser instanceof User) {
                return (User) sUser;
            }
            Object uid = session.getAttribute("userId");
            if (uid != null) {
                try {
                    return userRepository.findById(Long.valueOf(uid.toString())).orElse(null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @GetMapping({"/coolclub", "/hoi-vien"})
    public String viewCoolClub(Principal principal, HttpSession session, Model model) {
        User user = getAuthenticatedUser(principal, session);
        if (user != null) {
            user = userRepository.findById(user.getId()).orElse(user);
        }

        BigDecimal totalSpent = (user != null && user.getTotalSpent() != null) ? user.getTotalSpent() : BigDecimal.ZERO;
        String currentTier = (user != null && user.getMembershipTier() != null) ? user.getMembershipTier() : "NEW";

        String nextTier = "SILVER";
        BigDecimal targetSpent = BigDecimal.valueOf(1000000);
        int progressPercent = 0;
        BigDecimal remaining = BigDecimal.ZERO;

        if (totalSpent.compareTo(BigDecimal.valueOf(1000000)) < 0) {
            currentTier = "NEW";
            nextTier = "SILVER";
            targetSpent = BigDecimal.valueOf(1000000);
            progressPercent = totalSpent.multiply(BigDecimal.valueOf(100)).divide(targetSpent, 0, java.math.RoundingMode.DOWN).intValue();
            remaining = targetSpent.subtract(totalSpent);
        } else if (totalSpent.compareTo(BigDecimal.valueOf(3000000)) < 0) {
            currentTier = "SILVER";
            nextTier = "GOLD";
            targetSpent = BigDecimal.valueOf(3000000);
            BigDecimal diff = totalSpent.subtract(BigDecimal.valueOf(1000000));
            progressPercent = diff.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(2000000), 0, java.math.RoundingMode.DOWN).intValue();
            remaining = targetSpent.subtract(totalSpent);
        } else if (totalSpent.compareTo(BigDecimal.valueOf(6000000)) < 0) {
            currentTier = "GOLD";
            nextTier = "PLATINUM";
            targetSpent = BigDecimal.valueOf(6000000);
            BigDecimal diff = totalSpent.subtract(BigDecimal.valueOf(3000000));
            progressPercent = diff.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(3000000), 0, java.math.RoundingMode.DOWN).intValue();
            remaining = targetSpent.subtract(totalSpent);
        } else {
            currentTier = "PLATINUM";
            nextTier = "MAX";
            progressPercent = 100;
            remaining = BigDecimal.ZERO;
        }

        List<Promotion> exclusivePromotions = promotionRepository.findByIsActiveTrueOrderByStartDateDesc();

        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("currentTier", currentTier);
        model.addAttribute("nextTier", nextTier);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("targetSpent", targetSpent);
        model.addAttribute("progressPercent", Math.min(100, Math.max(0, progressPercent)));
        model.addAttribute("remainingToNextTier", remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO);
        model.addAttribute("exclusivePromotions", exclusivePromotions);

        return "coolclub";
    }
}