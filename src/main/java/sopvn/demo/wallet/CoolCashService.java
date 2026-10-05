package sopvn.demo.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.entity.CoolCashTransaction;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.User;
import sopvn.demo.repository.CoolCashTransactionRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CoolCashService {

    private final UserRepository userRepository;
    private final CoolCashTransactionRepository coolCashTransactionRepository;

    public CoolCashService(UserRepository userRepository, CoolCashTransactionRepository coolCashTransactionRepository) {
        this.userRepository = userRepository;
        this.coolCashTransactionRepository = coolCashTransactionRepository;
    }

    @Transactional(readOnly = true)
    public List<CoolCashTransaction> getUserTransactions(Long userId) {
        return coolCashTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void rewardOrderCashback(User user, Order order, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        user.setCoolcashBalance(user.getCoolcashBalance().add(amount));
        userRepository.save(user);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(user);
        tx.setOrder(order);
        tx.setAmount(amount);
        tx.setTransactionType("EARN_ORDER");
        tx.setStatus("COMPLETED");
        tx.setDescription("Hoàn tiền CoolCash từ đơn hàng #" + order.getOrderCode());
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }
}
