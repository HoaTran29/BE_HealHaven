package backend.healhaven.repository;

import backend.healhaven.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Integer> {

    List<Media> findByTargetIdAndTargetType(Integer targetId, String targetType);

    List<Media> findByTargetIdAndTargetTypeAndIsPrimaryTrue(Integer targetId, String targetType);
}
