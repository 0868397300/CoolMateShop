package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.UserAddress;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserId(Long userId);

    List<UserAddress> findByUserIdOrderByIdDesc(Long userId);

    List<UserAddress> findByUserIdOrderByIsDefaultDesc(Long userId);
}
