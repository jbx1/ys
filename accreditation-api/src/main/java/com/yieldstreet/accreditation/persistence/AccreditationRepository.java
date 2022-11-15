package com.yieldstreet.accreditation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccreditationRepository extends JpaRepository<Accreditation, UUID>, JpaSpecificationExecutor<Accreditation> {

    List<Accreditation> findByUserUserId(String userId);

    @Modifying
    @Query("update Accreditation a set a.status = :status, a.updatedTs = CURRENT_TIMESTAMP where a.id = :accreditationId and a.updatedTs = :updatedTs")
    int finaliseAccreditationStatus(@Param("status") Accreditation.AccreditationStatus status,
                                    @Param("accreditationId") UUID accreditationId,
                                    @Param("updatedTs") OffsetDateTime updatedTs);
}