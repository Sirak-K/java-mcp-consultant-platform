package mcp.server.domain.missions.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mission_proposal", schema = "marketplace", indexes = {
        @Index(name = "idx_mission_proposal_status", columnList = "mission_proposal_review_status"),
        @Index(name = "idx_mission_proposal_created_at", columnList = "created_at")
})
public class MissionProposalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_proposal_id")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    @Column(name = "mission_title", nullable = false, length = 200)
    private String missionTitle;

    @Column(name = "mission_start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "mission_end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "work_mode", nullable = false, length = 20)
    private String workMode;

    @Column(name = "presentation_one_day_at_work", nullable = false, columnDefinition = "TEXT")
    private String presentationOneDayAtWork;

    @Column(name = "presentation_technical_landscape", nullable = false, columnDefinition = "TEXT")
    private String presentationTechnicalLandscape;

    @Column(name = "presentation_who_we_are_looking_for", nullable = false, columnDefinition = "TEXT")
    private String presentationWhoWeAreLookingFor;

    @Column(name = "presentation_what_we_offer", nullable = false, columnDefinition = "TEXT")
    private String presentationWhatWeOffer;

    @Column(name = "presentation_about_customer", nullable = false, columnDefinition = "TEXT")
    private String presentationAboutCustomer;

    @Column(name = "presentation_recruitment_process", nullable = false, columnDefinition = "TEXT")
    private String presentationRecruitmentProcess;

    @Column(name = "mission_proposal_review_status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "missionProposal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MissionProposalSlotEntity> missionSlots = new ArrayList<>();

    protected MissionProposalEntity() {
    }

    public MissionProposalEntity(
            Long id,
            Long customerId,
            String customerName,
            String customerEmail,
            String missionTitle,
            LocalDate startDate,
            LocalDate endDate,
            String workMode,
            String presentationOneDayAtWork,
            String presentationTechnicalLandscape,
            String presentationWhoWeAreLookingFor,
            String presentationWhatWeOffer,
            String presentationAboutCustomer,
            String presentationRecruitmentProcess,
            String status,
            Instant createdAt,
            Instant updatedAt,
            List<MissionProposalSlotEntity> missionSlots) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.missionTitle = missionTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.workMode = workMode;
        this.presentationOneDayAtWork = presentationOneDayAtWork;
        this.presentationTechnicalLandscape = presentationTechnicalLandscape;
        this.presentationWhoWeAreLookingFor = presentationWhoWeAreLookingFor;
        this.presentationWhatWeOffer = presentationWhatWeOffer;
        this.presentationAboutCustomer = presentationAboutCustomer;
        this.presentationRecruitmentProcess = presentationRecruitmentProcess;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.missionSlots = missionSlots != null ? new ArrayList<>(missionSlots) : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getMissionTitle() {
        return missionTitle;
    }

    public void setMissionTitle(String missionTitle) {
        this.missionTitle = missionTitle;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    public String getPresentationOneDayAtWork() {
        return presentationOneDayAtWork;
    }

    public void setPresentationOneDayAtWork(String presentationOneDayAtWork) {
        this.presentationOneDayAtWork = presentationOneDayAtWork;
    }

    public String getPresentationTechnicalLandscape() {
        return presentationTechnicalLandscape;
    }

    public void setPresentationTechnicalLandscape(String presentationTechnicalLandscape) {
        this.presentationTechnicalLandscape = presentationTechnicalLandscape;
    }

    public String getPresentationWhoWeAreLookingFor() {
        return presentationWhoWeAreLookingFor;
    }

    public void setPresentationWhoWeAreLookingFor(String presentationWhoWeAreLookingFor) {
        this.presentationWhoWeAreLookingFor = presentationWhoWeAreLookingFor;
    }

    public String getPresentationWhatWeOffer() {
        return presentationWhatWeOffer;
    }

    public void setPresentationWhatWeOffer(String presentationWhatWeOffer) {
        this.presentationWhatWeOffer = presentationWhatWeOffer;
    }

    public String getPresentationAboutCustomer() {
        return presentationAboutCustomer;
    }

    public void setPresentationAboutCustomer(String presentationAboutCustomer) {
        this.presentationAboutCustomer = presentationAboutCustomer;
    }

    public String getPresentationRecruitmentProcess() {
        return presentationRecruitmentProcess;
    }

    public void setPresentationRecruitmentProcess(String presentationRecruitmentProcess) {
        this.presentationRecruitmentProcess = presentationRecruitmentProcess;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<MissionProposalSlotEntity> getMissionSlots() {
        return missionSlots;
    }

    public void setMissionSlots(List<MissionProposalSlotEntity> missionSlots) {
        this.missionSlots = missionSlots != null ? new ArrayList<>(missionSlots) : new ArrayList<>();
        this.missionSlots.forEach(slot -> slot.setMissionProposal(this));
    }

    public void replaceMissionSlots(List<MissionProposalSlotEntity> missionSlots) {
        this.missionSlots.clear();
        if (missionSlots != null) {
            missionSlots.forEach(this::addMissionSlot);
        }
    }

    public void addMissionSlot(MissionProposalSlotEntity slot) {
        if (slot != null) {
            slot.setMissionProposal(this);
            this.missionSlots.add(slot);
        }
    }
}
