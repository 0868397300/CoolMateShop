package sopvn.demo.wallet;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import sopvn.demo.entity.CoolCashTransaction;
import sopvn.demo.repository.CoolCashTransactionRepository;

import java.util.List;

@Controller
@RequestMapping("/vi-coolcash")
public class CoolCashController {

    private final CoolCashTransactionRepository coolCashTransactionRepository;

    public CoolCashController(CoolCashTransactionRepository coolCashTransactionRepository) {
        this.coolCashTransactionRepository = coolCashTransactionRepository;
    }

    @GetMapping
    public String walletPage(Model model) {
        List<CoolCashTransaction> transactions = coolCashTransactionRepository.findAll();
        model.addAttribute("transactions", transactions);
        return "client/wallet";
    }
}
