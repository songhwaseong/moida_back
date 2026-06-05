package com.moida.domain.audit;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "admin_action_logs",
        indexes = {
                @Index(name = "idx_admin_action_log_admin", columnList = "admin_id"),
                @Index(name = "idx_admin_action_log_action", columnList = "action_type"),
                @Index(name = "idx_admin_action_log_target", columnList = "target_type,target_id"),
                @Index(name = "idx_admin_action_log_created_at", columnList = "created_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_action_log_id")
    private Long id;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_email", nullable = false, length = 255)
    private String adminEmail;

    @Column(name = "action_type", nullable = false, length = 60)
    private String actionType;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Lob
    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Lob
    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @Column(length = 500)
    private String reason;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Builder
    private AdminActionLog(Long adminId, String adminEmail, String actionType, String targetType,
                           Long targetId, String targetName, String beforeValue, String afterValue,
                           String reason, String ip, String userAgent) {
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.reason = reason;
        this.ip = ip;
        this.userAgent = userAgent;
    }
}
