package sopvn.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.OrderReturn;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OrderReturn r WHERE r.id = :id")
    Optional<OrderReturn> findByIdForUpdate(@Param("id") Long id);

    List<OrderReturn> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<OrderReturn> findByOrderId(Long orderId);

    List<OrderReturn> findByOrderItemId(Long orderItemId);

    List<OrderReturn> findAllByOrderByCreatedAtDesc();
}