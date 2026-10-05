package sopvn.demo.wallet;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sopvn.demo.entity.CoolCashTransaction;
import sopvn.demo.entity.Promotion;
import sopvn.demo.entity.User;
import sopvn.demo.repository.PromotionRepository;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;
import java.util.List;

@Controller
public class WalletController {

    private final CoolCashService coolCashService;
    private final UserRepository userRepository;
    private final PromotionRepository promotionRepository;

    public WalletController(CoolCashService coolCashService,
                            UserRepository userRepository,
                            PromotionRepository promotionRepository) {
        this.coolCashService = coolCashService;
        this.userRepository = userRepository;
        this.promotionRepository = promotionRepository;
    }

    private User getAuthenticatedUser(Principal principal, HttpSession session) {
        if (principal != null) {
            return userRepository.findByEmail(principal.getName()).orElse(null);
        }
        if (session != null) {
            Object sUser = session.getAttribute("currentUser");
            if (sUser instanceof User) return (User) sUser;
            Object uid = session.getAttribute("userId");
            if (uid != null) {
                try {
                    return userRepository.findById(Long.valueOf(uid.toString())).orElse(null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @GetMapping({"/vi-coolcash", "/tai-khoan/vi-coolcash"})
    public String viewWallet(Principal principal, HttpSession session, Model model) {
        User user = getAuthenticatedUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login?required=wallet";
        }

        user = userRepository.findById(user.getId()).orElse(user);

        List<CoolCashTransaction> transactions = coolCashService.getUserTransactions(user.getId());
        List<Promotion> promotions = promotionRepository.findByIsActiveTrueOrderByStartDateDesc();

        model.addAttribute("currentUser", user);
        model.addAttribute("user", user);
        model.addAttribute("transactions", transactions);
        model.addAttribute("promotions", promotions);

        return "client/wallet";
    }
}
