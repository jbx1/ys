package com.yieldstreet.accreditation.persistence;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accreditations")
public class Accreditation implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum AccreditationType {
    BY_INCOME,
    BY_NET_WORTH
  }

  public enum AccreditationStatus {
    PENDING,
    CONFIRMED,
    EXPIRED,
    FAILED
  }

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private AccreditationType type;

  @Column(name = "document_name", nullable = false)
  private String documentName;

  @Column(name = "document_mime_type", nullable = false)
  private String documentMimeType;

  @Column(name = "document_content", nullable = false)
  private String documentContent;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private AccreditationStatus status;

  @Column(name = "created_ts", nullable = false)
  private OffsetDateTime createdTs;

  @Column(name = "updated_ts", nullable = false)
  private OffsetDateTime updatedTs;

  @PrePersist
  public void onPrePersist() {
    this.createdTs = OffsetDateTime.now();
    this.updatedTs = OffsetDateTime.now();
  }

  @PreUpdate
  public void onPreUpdate() {
    this.updatedTs = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public void setType(AccreditationType type) {
    this.type = type;
  }

  public AccreditationType getType() {
    return type;
  }

  public void setDocumentName(String documentName) {
    this.documentName = documentName;
  }

  public String getDocumentName() {
    return documentName;
  }

  public void setDocumentMimeType(String documentMimeType) {
    this.documentMimeType = documentMimeType;
  }

  public String getDocumentMimeType() {
    return documentMimeType;
  }

  public void setDocumentContent(String documentContent) {
    this.documentContent = documentContent;
  }

  public String getDocumentContent() {
    return documentContent;
  }

  public void setStatus(AccreditationStatus status) {
    this.status = status;
  }

  public AccreditationStatus getStatus() {
    return status;
  }

  public void setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public void setUpdatedTs(OffsetDateTime updatedTs) {
    this.updatedTs = updatedTs;
  }

  public OffsetDateTime getUpdatedTs() {
    return updatedTs;
  }

  @Override
  public String toString() {
    return "Accreditation{"
        + "id="
        + id
        + '\''
        + "userId="
        + (user == null ? "null" : user.getUserId())
        + '\''
        + "type="
        + type
        + '\''
        + "documentName="
        + documentName
        + '\''
        + "documentMimeType="
        + documentMimeType
        + '\''
        + "documentContent="
        + documentContent
        + '\''
        + "status="
        + status
        + '\''
        + "createdTs="
        + createdTs
        + '\''
        + "updatedTs="
        + updatedTs
        + '\''
        + '}';
  }
}
