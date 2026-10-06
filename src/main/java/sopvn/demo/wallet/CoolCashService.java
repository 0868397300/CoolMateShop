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
import java.util.Optional;

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
        if (coolCashTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return; // Đã nhận cashback cho đơn hàng này rồi
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
        tx.setIdempotencyKey(idempotencyKey);
        tx.setDescription("Hoàn tiền " + cashbackRate + "% CoolClub đơn #" + order.getOrderCode());
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }

    @Transactional
    public void revokeOrderCashback(User user, Order order, BigDecimal amount, String reason, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (user == null) return;

        String key = idempotencyKey != null ? idempotencyKey : ("ORDER_CASHBACK_REVOKE:" + (order != null ? order.getId() : "MANUAL"));
        if (coolCashTransactionRepository.existsByIdempotencyKey(key)) {
            return; // Đã thu hồi rồi, không trừ lặp
        }

        BigDecimal current = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;
        BigDecimal newBal = current.subtract(amount);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) newBal = BigDecimal.ZERO;
        user.setCoolcashBalance(newBal);
        userRepository.save(user);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(user);
        tx.setOrder(order);
        tx.setAmount(amount.negate());
        tx.setTransactionType("REVOKE_RETURN");
        tx.setStatus("COMPLETED");
        tx.setIdempotencyKey(key);
        tx.setDescription((reason != null ? reason : "Thu hồi hoàn tiền do đổi trả sản phẩm") + " [" + key + "]");
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }

    @Transactional
    public void reserveCoolCash(User user, Order order, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (user == null) return;

        String key = idempotencyKey != null ? idempotencyKey : ("ORDER_COOLCASH_RESERVE:" + (order != null ? order.getId() : "MANUAL"));
        if (coolCashTransactionRepository.existsByIdempotencyKey(key)) {
            return; // Đã giữ rồi
        }

        BigDecimal current = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;
        if (current.compareTo(amount) < 0) {
            throw new CustomException("Số dư ví CoolCash không đủ (Số dư: " + current + "đ, yêu cầu: " + amount + "đ).");
        }

        user.setCoolcashBalance(current.subtract(amount));
        userRepository.save(user);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(user);
        tx.setOrder(order);
        tx.setAmount(amount.negate());
        tx.setTransactionType("SPEND_ORDER");
        tx.setStatus("RESERVED");
        tx.setIdempotencyKey(key);
        tx.setDescription("Tạm giữ thanh toán đơn hàng #" + (order != null ? order.getOrderCode() : "") + " [" + key + "]");
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }

    @Transactional
    public void finalizeCoolCashSpend(User user, Order order, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (order == null) return;

        String reserveKey = "ORDER_COOLCASH_RESERVE:" + order.getId();
        Optional<CoolCashTransaction> reserveOpt = coolCashTransactionRepository.findByIdempotencyKey(reserveKey);
        if (reserveOpt.isPresent()) {
            CoolCashTransaction tx = reserveOpt.get();
            tx.setStatus("COMPLETED");
            tx.setDescription("Thanh toán thành công đơn hàng #" + order.getOrderCode() + " [" + idempotencyKey + "]");
            coolCashTransactionRepository.save(tx);
            return;
        }

        // Nếu chưa reserve thì trừ trực tiếp
        spendCoolCash(user, order, amount, idempotencyKey);
    }

    @Transactional
    public void releaseReservedCoolCash(User user, Order order, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (user == null || order == null) return;

        String releaseKey = idempotencyKey != null ? idempotencyKey : ("ORDER_COOLCASH_RELEASE:" + order.getId());
        if (coolCashTransactionRepository.existsByIdempotencyKey(releaseKey)) {
            return; // Đã giải phóng rồi
        }

        String reserveKey = "ORDER_COOLCASH_RESERVE:" + order.getId();
        Optional<CoolCashTransaction> reserveOpt = coolCashTransactionRepository.findByIdempotencyKey(reserveKey);
        if (reserveOpt.isPresent()) {
            CoolCashTransaction tx = reserveOpt.get();
            tx.setStatus("RELEASED");
            coolCashTransactionRepository.save(tx);
        }

        // Hoàn lại tiền vào số dư cho user
        BigDecimal current = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;
        user.setCoolcashBalance(current.add(amount));
        userRepository.save(user);

        CoolCashTransaction refundTx = new CoolCashTransaction();
        refundTx.setUser(user);
        refundTx.setOrder(order);
        refundTx.setAmount(amount);
        refundTx.setTransactionType("REFUND_ORDER");
        refundTx.setStatus("COMPLETED");
        refundTx.setIdempotencyKey(releaseKey);
        refundTx.setDescription("Giải phóng tiền tạm giữ đơn hàng VNPAY thất bại #" + order.getOrderCode());
        refundTx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(refundTx);
    }

    @Transactional
    public void spendCoolCash(User user, Order order, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (user == null) return;

        String key = idempotencyKey != null ? idempotencyKey : ("ORDER_COOLCASH_SPEND:" + (order != null ? order.getId() : "MANUAL"));
        if (coolCashTransactionRepository.existsByIdempotencyKey(key)) {
            return; // Đã trừ rồi, bỏ qua idempotent
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
        tx.setIdempotencyKey(key);
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
        if (coolCashTransactionRepository.existsByIdempotencyKey(key)) {
            return; // Đã hoàn tiền rồi, bỏ qua
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
        tx.setIdempotencyKey(key);
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
        tx.setIdempotencyKey("ADMIN_ADJUST:" + System.currentTimeMillis() + ":" + user.getId());
        tx.setDescription("Admin điều chỉnh ví: " + (reason != null ? reason : ""));
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }
}
