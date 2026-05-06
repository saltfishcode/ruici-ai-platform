package com.ruici.ai.modules.voice.model;

import com.ruici.ai.common.config.runtime.AiRuntimeConfigSnapshot;
import com.ruici.ai.common.config.runtime.AiRuntimeConfigSource;
import com.ruici.ai.common.config.runtime.AiRuntimeDomain;
import com.ruici.ai.common.config.runtime.AiRuntimeScene;
import com.ruici.ai.common.model.AsyncTaskStatus;
import com.ruici.ai.common.constant.CommonConstants.ScenarioDefaults;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 语音场景会话实体。
 *
 * <p>表名仍保留 `voice_interview_sessions` 历史命名，以兼容已有数据库。</p>
 */
@Entity
@Table(name = "voice_interview_sessions", indexes = {
    @Index(name = "idx_voice_session_user_updated", columnList = "user_id,updated_at"),
    @Index(name = "idx_voice_session_status_updated", columnList = "status,updated_at"),
    @Index(name = "idx_voice_session_skill_created", columnList = "skill_id,created_at"),
    @Index(name = "idx_voice_session_resume", columnList = "resume_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceInterviewSessionEntity {

    private static final String CHAT_CONFIG_KEY = "THIRD_PARTY_MODEL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "role_type", nullable = false)
    private String roleType;

    @Column(name = "skill_id", length = 64)
    @Builder.Default
    private String skillId = ScenarioDefaults.SKILL_ID;

    @Column(name = "difficulty", length = 16)
    @Builder.Default
    private String difficulty = ScenarioDefaults.DIFFICULTY;

    @Column(name = "custom_jd_text", columnDefinition = "TEXT")
    private String customJdText;

    @Column(name = "resume_id")
    private Long resumeId;

    @Column(name = "intro_enabled")
    @Builder.Default
    private Boolean introEnabled = true;

    @Column(name = "tech_enabled")
    @Builder.Default
    private Boolean techEnabled = true;

    @Column(name = "project_enabled")
    @Builder.Default
    private Boolean projectEnabled = true;

    @Column(name = "hr_enabled")
    @Builder.Default
    private Boolean hrEnabled = true;

    @Column(name = "llm_provider", length = 50)
    @Builder.Default
    private String llmProvider = ScenarioDefaults.LLM_PROVIDER;

    @Column(name = "llm_model_name", length = 128)
    private String llmModelName;

    @Column(name = "llm_fallback_model_name", length = 128)
    private String llmFallbackModelName;

    @Column(name = "llm_config_version")
    private Long llmConfigVersion;

    @Column(name = "llm_config_source", length = 64)
    private String llmConfigSource;

    @Column(name = "llm_config_stale")
    @Builder.Default
    private Boolean llmConfigStale = false;

    @Column(name = "current_phase")
    @Enumerated(EnumType.STRING)
    private InterviewPhase currentPhase;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VoiceInterviewSessionStatus status = VoiceInterviewSessionStatus.IN_PROGRESS;

    @Column(name = "planned_duration")
    @Builder.Default
    private Integer plannedDuration = 30;

    @Column(name = "actual_duration")
    private Integer actualDuration;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    @Column(name = "evaluate_status")
    @Enumerated(EnumType.STRING)
    private AsyncTaskStatus evaluateStatus;

    @Column(name = "evaluate_error", length = 500)
    private String evaluateError;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.startTime = LocalDateTime.now();
    }

    public enum InterviewPhase {
        INTRO, TECH, PROJECT, HR, COMPLETED
    }

    /**
     * 语义化别名：在通用语音场景里，`resumeId` 实际表示关联文档 ID。
     */
    @Transient
    public Long getDocumentId() {
        return resumeId;
    }

    public void applyLlmRuntimeSnapshot(AiRuntimeConfigSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.llmProvider = snapshot.providerId();
        this.llmModelName = snapshot.modelName();
        this.llmFallbackModelName = snapshot.fallbackModelName();
        this.llmConfigVersion = snapshot.configVersion();
        this.llmConfigSource = snapshot.source() != null ? snapshot.source().name() : null;
        this.llmConfigStale = snapshot.stale();
    }

    public boolean hasLlmRuntimeSnapshot() {
        return llmProvider != null
            && !llmProvider.isBlank()
            && llmModelName != null
            && !llmModelName.isBlank()
            && llmConfigVersion != null;
    }

    public AiRuntimeConfigSnapshot toLlmRuntimeSnapshot() {
        if (!hasLlmRuntimeSnapshot()) {
            return null;
        }
        return new AiRuntimeConfigSnapshot(
            CHAT_CONFIG_KEY,
            AiRuntimeDomain.CHAT,
            AiRuntimeScene.VOICE,
            llmProvider,
            llmModelName,
            llmFallbackModelName,
            llmConfigVersion,
            llmConfigSource != null ? AiRuntimeConfigSource.valueOf(llmConfigSource) : null,
            Boolean.TRUE.equals(llmConfigStale)
        );
    }
}
