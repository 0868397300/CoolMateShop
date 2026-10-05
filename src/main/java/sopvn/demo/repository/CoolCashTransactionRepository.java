package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.CoolCashTransaction;

import java.util.List;

@Repository
public interface CoolCashTransactionRepository extends JpaRepository<CoolCashTransaction, Long> {

    List<CoolCashTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
