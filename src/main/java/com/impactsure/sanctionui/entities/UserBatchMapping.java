package com.impactsure.sanctionui.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_batch_mapping",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_batch", columnNames = {"user_id", "batch_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBatchMapping extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;
}
