package mcp.server.domain.missions.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "mission_proposal_outcome", schema = "consultant_platform")
public class MissionProposalOutcomeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_proposal_outcome_id")
    private Long id;

    @Column(name = "mission_proposal_id", nullable = false, insertable = false, updatable = false)
    private Long missionProposalId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_proposal_id", nullable = false)
    private MissionProposalEntity missionProposal;

    @Column(name = "outcome_status", nullable = false, length = 20)
    private String outcomeStatus;

    @Column(name = "outcome_note")
    private String outcomeNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MissionProposalOutcomeEntity() {
    }

    public MissionProposalOutcomeEntity(
            Long id,
            MissionProposalEntity missionProposal,
            String outcomeStatus,
            String outcomeNote,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.missionProposal = missionProposal;
        this.outcomeStatus = outcomeStatus;
        this.outcomeNote = outcomeNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMissionProposalId() {
        return missionProposalId;
    }

    public void setMissionProposalId(Long missionProposalId) {
        this.missionProposalId = missionProposalId;
    }

    public MissionProposalEntity getMissionProposal() {
        return missionProposal;
    }

    public void setMissionProposal(MissionProposalEntity missionProposal) {
        this.missionProposal = missionProposal;
    }

    public String getOutcomeStatus() {
        return outcomeStatus;
    }

    public void setOutcomeStatus(String outcomeStatus) {
        this.outcomeStatus = outcomeStatus;
    }

    public String getOutcomeNote() {
        return outcomeNote;
    }

    public void setOutcomeNote(String outcomeNote) {
        this.outcomeNote = outcomeNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
