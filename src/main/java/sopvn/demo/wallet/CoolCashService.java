package sopvn.demo.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.core.exception.CustomException;
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
    public void rewardOrderCashback(User user, Order order, BigDecimal amount, int cashbackRate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (user == null || order == null) return;

        String idempotencyKey = "ORDER_CASHBACK:" + order.getId();
        List<CoolCashTransaction> existing = coolCashTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        for (CoolCashTransaction tx : existing) {
            if (tx.getDescription() != null && tx.getDescription().contains(idempotencyKey)) {
                return; // Đã nhận rồi, bỏ qua không nhân đôi
            }
        }

        BigDecimal current = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;
        user.setCoolcashBalance(current.add(amount));
        userRepository.save(user);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(user);
        tx.setOrder(order);
        tx.setAmount(amount);
        tx.setTransactionType("EARN_ORDER");
        tx.setStatus("COMPLETED");
        tx.setDescription("Hoàn tiền " + cashbackRate + "% CoolClub đơn #" + order.getOrderCode() + " [" + idempotencyKey + "]");
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }

    @Transactional
    public void spendCoolCash(User user, Order order, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (user == null) return;

        String key = idempotencyKey != null ? idempotencyKey : ("ORDER_COOLCASH_SPEND:" + (order != null ? order.getId() : "MANUAL"));
        List<CoolCashTransaction> existing = coolCashTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        for (CoolCashTransaction tx : existing) {
            if (tx.getDescription() != null && tx.getDescription().contains(key)) {
                return; // Đã trừ rồi, bỏ qua idempotent
            }
        }

        BigDecimal current = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;
        if (current.compareTo(amount) < 0) {
            throw new CustomException("Số dư ví CoolCash không đủ (Số dư hiện tại: " + current + "đ, yêu cầu: " + amount + "đ).");
        }

        user.setCoolcashBalance(current.subtract(amount));
        userRepository.save(user);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(user);
        tx.setOrder(order);
        tx.setAmount(amount.negate());
        tx.setTransactionType("SPEND_ORDER");
        tx.setStatus("COMPLETED");
        tx.setDescription("Thanh toán đơn hàng #" + (order != null ? order.getOrderCode() : "") + " [" + key + "]");
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }

    @Transactional
    public void spendCoolCash(User user, Order order, BigDecimal amount) {
        spendCoolCash(user, order, amount, null);
    }

    @Transactional
    public void refundCoolCash(User user, Order order, BigDecimal amount, String reason, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (user == null) return;

        String key = idempotencyKey != null ? idempotencyKey : ("ORDER_COOLCASH_REFUND:" + (order != null ? order.getId() : "MANUAL"));
        List<CoolCashTransaction> existing = coolCashTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        for (CoolCashTransaction tx : existing) {
            if (tx.getDescription() != null && tx.getDescription().contains(key)) {
                return; // Đã hoàn tiền rồi, bỏ qua
            }
        }

        BigDecimal current = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;
        user.setCoolcashBalance(current.add(amount));
        userRepository.save(user);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(user);
        tx.setOrder(order);
        tx.setAmount(amount);
        tx.setTransactionType("REFUND_RETURN");
        tx.setStatus("COMPLETED");
        tx.setDescription((reason != null ? reason : "Hoàn tiền ví CoolCash") + " [" + key + "]");
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }

    @Transactional
    public void refundCoolCash(User user, Order order, BigDecimal amount, String reason) {
        refundCoolCash(user, order, amount, reason, null);
    }

    @Transactional
    public void adminAdjust(User user, BigDecimal amount, String type, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (user == null) return;

        BigDecimal actualAmount = "MINUS".equalsIgnoreCase(type) ? amount.negate() : amount;
        BigDecimal current = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;
        BigDecimal newBal = current.add(actualAmount);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) newBal = BigDecimal.ZERO;

        user.setCoolcashBalance(newBal);
        userRepository.save(user);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(user);
        tx.setAmount(actualAmount);
        tx.setTransactionType("ADMIN_ADJUST");
        tx.setStatus("COMPLETED");
        tx.setDescription("Admin điều chỉnh ví: " + (reason != null ? reason : ""));
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }
}
