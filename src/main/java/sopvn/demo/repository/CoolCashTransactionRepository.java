package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.CoolCashTransaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoolCashTransactionRepository extends JpaRepository<CoolCashTransaction, Long> {

    List<CoolCashTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<CoolCashTransaction> findByIdempotencyKey(String idempotencyKey);
}
