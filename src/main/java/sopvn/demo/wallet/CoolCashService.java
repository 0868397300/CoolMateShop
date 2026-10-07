package sopvn.demo.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CoolCashService.class);

    private final UserRepository userRepository;
    private final CoolCashTransactionRepository coolCashTransactionRepository;

    public CoolCashService(UserRepository userRepository, CoolCashTransactionRepository coolCashTransactionRepository) {
        this.userRepository = userRepository;
        this.coolCashTransactionRepository = coolCashTransactionRepository;
    }

    private User findUserWithLock(Long userId, User fallback) {
        if (userId == null) return fallback;
        Optional<User> locked = userRepository.findByIdForUpdate(userId);
        if (locked.isPresent()) {
            return locked.get();
        }
        return userRepository.findById(userId).orElse(fallback);
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

        User lockedUser = findUserWithLock(user.getId(), user);
        BigDecimal current = lockedUser.getCoolcashBalance() != null ? lockedUser.getCoolcashBalance() : BigDecimal.ZERO;
        BigDecimal newBal = current.add(amount);
        lockedUser.setCoolcashBalance(newBal);
        userRepository.save(lockedUser);
        user.setCoolcashBalance(newBal);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(lockedUser);
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

        User lockedUser = findUserWithLock(user.getId(), user);
        BigDecimal current = lockedUser.getCoolcashBalance() != null ? lockedUser.getCoolcashBalance() : BigDecimal.ZERO;
        BigDecimal newBal = current.subtract(amount);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) newBal = BigDecimal.ZERO;
        lockedUser.setCoolcashBalance(newBal);
        userRepository.save(lockedUser);
        user.setCoolcashBalance(newBal);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(lockedUser);
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

        User lockedUser = findUserWithLock(user.getId(), user);
        BigDecimal current = lockedUser.getCoolcashBalance() != null ? lockedUser.getCoolcashBalance() : BigDecimal.ZERO;
        if (current.compareTo(amount) < 0) {
            throw new CustomException("Số dư ví CoolCash không đủ (Số dư: " + current + "đ, yêu cầu: " + amount + "đ).");
        }

        BigDecimal newBal = current.subtract(amount);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("Số dư ví không thể âm.");
        }
        lockedUser.setCoolcashBalance(newBal);
        userRepository.save(lockedUser);
        user.setCoolcashBalance(newBal);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(lockedUser);
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
        
        // P0-7: Bắt buộc phải có giao dịch tạm giữ hợp lệ trước đó, không được âm thầm fallback sang direct spend
        if (reserveOpt.isEmpty()) {
            throw new CustomException("Không tìm thấy giao dịch tạm giữ CoolCash hợp lệ (RESERVED) để hoàn tất cho đơn hàng #" + order.getOrderCode());
        }

        CoolCashTransaction tx = reserveOpt.get();
        if ("COMPLETED".equalsIgnoreCase(tx.getStatus())) {
            return; // Idempotent
        }

        if (!"RESERVED".equalsIgnoreCase(tx.getStatus())) {
            throw new CustomException("Giao dịch CoolCash đơn #" + order.getOrderCode() + " không ở trạng thái tạm giữ hợp lệ (Hiện tại: " + tx.getStatus() + ")");
        }

        tx.setStatus("COMPLETED");
        tx.setDescription("Thanh toán thành công đơn hàng #" + order.getOrderCode() + " [" + idempotencyKey + "]");
        coolCashTransactionRepository.save(tx);
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
        
        // P0-7: release phải kiểm tra có matching reservation. Tuyệt đối không tự sinh ra tiền khi không có reservation khớp.
        if (reserveOpt.isEmpty()) {
            log.warn("Không tìm thấy giao dịch tạm giữ CoolCash cho đơn hàng #{}, từ chối giải phóng để ngăn tạo tiền khống.", order.getOrderCode());
            return;
        }

        CoolCashTransaction reserveTx = reserveOpt.get();
        if ("RELEASED".equalsIgnoreCase(reserveTx.getStatus())) {
            return; // Idempotent
        }

        if (!"RESERVED".equalsIgnoreCase(reserveTx.getStatus())) {
            log.warn("Giao dịch CoolCash cho đơn hàng #{} không ở trạng thái RESERVED (Hiện tại: {}), từ chối giải phóng.", order.getOrderCode(), reserveTx.getStatus());
            return;
        }

        reserveTx.setStatus("RELEASED");
        coolCashTransactionRepository.save(reserveTx);

        // Hoàn lại tiền vào số dư cho user dưới pessimistic write lock
        User lockedUser = findUserWithLock(user.getId(), user);
        BigDecimal current = lockedUser.getCoolcashBalance() != null ? lockedUser.getCoolcashBalance() : BigDecimal.ZERO;
        BigDecimal newBal = current.add(amount);
        lockedUser.setCoolcashBalance(newBal);
        userRepository.save(lockedUser);
        user.setCoolcashBalance(newBal);

        CoolCashTransaction refundTx = new CoolCashTransaction();
        refundTx.setUser(lockedUser);
        refundTx.setOrder(order);
        refundTx.setAmount(amount);
        refundTx.setTransactionType("REFUND_ORDER");
        refundTx.setStatus("COMPLETED");
        refundTx.setIdempotencyKey(releaseKey);
        refundTx.setDescription("Giải phóng tiền tạm giữ đơn hàng VNPAY thất bại/hủy #" + order.getOrderCode());
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

        User lockedUser = findUserWithLock(user.getId(), user);
        BigDecimal current = lockedUser.getCoolcashBalance() != null ? lockedUser.getCoolcashBalance() : BigDecimal.ZERO;
        if (current.compareTo(amount) < 0) {
            throw new CustomException("Số dư ví CoolCash không đủ (Số dư hiện tại: " + current + "đ, yêu cầu: " + amount + "đ).");
        }

        BigDecimal newBal = current.subtract(amount);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("Số dư ví không thể âm.");
        }
        lockedUser.setCoolcashBalance(newBal);
        userRepository.save(lockedUser);
        user.setCoolcashBalance(newBal);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(lockedUser);
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

        User lockedUser = findUserWithLock(user.getId(), user);
        BigDecimal current = lockedUser.getCoolcashBalance() != null ? lockedUser.getCoolcashBalance() : BigDecimal.ZERO;
        BigDecimal newBal = current.add(amount);
        lockedUser.setCoolcashBalance(newBal);
        userRepository.save(lockedUser);
        user.setCoolcashBalance(newBal);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(lockedUser);
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

        User lockedUser = findUserWithLock(user.getId(), user);
        BigDecimal actualAmount = "MINUS".equalsIgnoreCase(type) ? amount.negate() : amount;
        BigDecimal current = lockedUser.getCoolcashBalance() != null ? lockedUser.getCoolcashBalance() : BigDecimal.ZERO;
        BigDecimal newBal = current.add(actualAmount);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) newBal = BigDecimal.ZERO;

        lockedUser.setCoolcashBalance(newBal);
        userRepository.save(lockedUser);
        user.setCoolcashBalance(newBal);

        CoolCashTransaction tx = new CoolCashTransaction();
        tx.setUser(lockedUser);
        tx.setAmount(actualAmount);
        tx.setTransactionType("ADMIN_ADJUST");
        tx.setStatus("COMPLETED");
        tx.setIdempotencyKey("ADMIN_ADJUST:" + System.currentTimeMillis() + ":" + user.getId());
        tx.setDescription("Admin điều chỉnh ví: " + (reason != null ? reason : ""));
        tx.setCreatedAt(LocalDateTime.now());
        coolCashTransactionRepository.save(tx);
    }
}
