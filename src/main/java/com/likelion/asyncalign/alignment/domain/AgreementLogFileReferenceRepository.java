package com.likelion.asyncalign.alignment.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgreementLogFileReferenceRepository extends JpaRepository<AgreementLogFileReference, UUID> {

    List<AgreementLogFileReference> findAllByAgreementLogId(UUID agreementLogId);
}
