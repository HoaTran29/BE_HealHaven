package backend.healhaven.repository;

import backend.healhaven.entity.WithdrawalRequestEntity;
import backend.healhaven.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequestEntity, Integer> {
    Page<WithdrawalRequestEntity> findByStatus(WithdrawalStatus status, Pageable pageable);
}
