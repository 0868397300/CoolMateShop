package sopvn.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByVnpayTxnRef(String vnpayTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.vnpayTxnRef = :vnpayTxnRef")
    Optional<Order> findByVnpayTxnRefForUpdate(@Param("vnpayTxnRef") String vnpayTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT o FROM Order o WHERE o.paymentMethod = 'VNPAY' AND o.paymentStatus = 'PAYMENT_PENDING' AND o.orderStatus = 'PENDING' AND o.createdAt <= :threshold")
    List<Order> findExpiredVnpayOrders(@Param("threshold") LocalDateTime threshold);

    Optional<Order> findByOrderCodeAndRecipientPhone(String orderCode, String recipientPhone);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    long countByOrderStatus(String orderStatus);
}
