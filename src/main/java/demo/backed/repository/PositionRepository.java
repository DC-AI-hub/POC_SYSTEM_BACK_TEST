package demo.backed.repository;

import demo.backed.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    /**
     * 根据岗位代码查询岗位
     */
    Optional<Position> findByCode(String code);
} 