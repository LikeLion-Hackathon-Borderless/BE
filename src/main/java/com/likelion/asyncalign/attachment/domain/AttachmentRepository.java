package com.likelion.asyncalign.attachment.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    @Query("""
            select attachment from Attachment attachment
            join fetch attachment.conversation
            join fetch attachment.uploader
            left join fetch attachment.message
            where attachment.id = :id
            """)
    Optional<Attachment> findWithDetailsById(@Param("id") UUID id);

    List<Attachment> findAllByMessageIdOrderByCreatedAtAsc(UUID messageId);

    List<Attachment> findAllByIdIn(List<UUID> ids);
}
