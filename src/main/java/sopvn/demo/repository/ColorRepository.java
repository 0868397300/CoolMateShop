package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.Color;

@Repository
public interface ColorRepository extends JpaRepository<Color, Integer> {
}
