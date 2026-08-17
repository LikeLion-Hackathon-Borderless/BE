package com.likelion.asyncalign.alignment.domain;

import com.likelion.asyncalign.attachment.domain.Attachment;
import com.likelion.asyncalign.global.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "agreement_log_file_references")
public class AgreementLogFileReference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_log_id", nullable = false)
    private AgreementLog agreementLog;

    @Column(nullable = false)
    private UUID attachmentId;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 255)
    private String locator;

    protected AgreementLogFileReference() {
    }

    public AgreementLogFileReference(AgreementLog log, Attachment attachment, String locator) {
        this.agreementLog = log;
        this.attachmentId = attachment.getId();
        this.fileName = attachment.getOriginalFileName();
        this.locator = locator;
    }

    public UUID getAttachmentId() { return attachmentId; }
    public String getFileName() { return fileName; }
    public String getLocator() { return locator; }
}
