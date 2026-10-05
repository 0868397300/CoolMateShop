package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.OrderReturn;

import java.util.List;

@Repository
public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {

    List<OrderReturn> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<OrderReturn> findByOrderId(Long orderId);

    List<OrderReturn> findByOrderItemId(Long orderItemId);

    List<OrderReturn> findAllByOrderByCreatedAtDesc();
}