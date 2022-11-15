package com.yieldstreet.accreditation.persistence;

import javax.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  /** The unique internal ID of the user. */
  @Id
  @SequenceGenerator(name = "users_id_seq", sequenceName = "users_id_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_seq")
  @Column(name = "id", nullable = false)
  private Integer id;

  /** The external unique ID of the user, known to the outside world. */
  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "created_ts", nullable = false)
  private OffsetDateTime createdTs;

  /** The unique internal ID of the user. */
  public void setId(Integer id) {
    this.id = id;
  }

  /** The unique internal ID of the user. */
  public Integer getId() {
    return id;
  }

  /** The external unique ID of the user, known to the outside world. */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /** The external unique ID of the user, known to the outside world. */
  public String getUserId() {
    return userId;
  }

  public void setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  @PrePersist
  public void onPrePersist() {
    this.createdTs = OffsetDateTime.now();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(id, user.id)
        && Objects.equals(userId, user.userId)
        && Objects.equals(createdTs, user.createdTs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userId, createdTs);
  }

  @Override
  public String toString() {
    return "User{" +
            "id=" + id +
            ", userId='" + userId + '\'' +
            ", createdTs=" + createdTs +
            '}';
  }
}
