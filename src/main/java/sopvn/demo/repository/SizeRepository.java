package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.Size;

import java.util.List;

@Repository
public interface SizeRepository extends JpaRepository<Size, Integer> {

    List<Size> findAllByOrderByDisplayOrderAsc();
}
