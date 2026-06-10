package mcp.server.domain.reference_data.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "competency_level_lookup", schema = "marketplace")
public class CompetencyLevelLookupEntity {

    @Id
    @Column(name = "competency_level_lookup_id")
    private Short competencyLevelLookupId;

    @Column(name = "competency_level_name", nullable = false, length = 20)
    private String competencyLevelName;

    @Column(name = "competency_min_years", nullable = false)
    private Short competencyMinYears;

    protected CompetencyLevelLookupEntity() {
    }

    public CompetencyLevelLookupEntity(Short competencyLevelLookupId, String competencyLevelName,
            Short competencyMinYears) {
        this.competencyLevelLookupId = competencyLevelLookupId;
        this.competencyLevelName = competencyLevelName;
        this.competencyMinYears = competencyMinYears;
    }

    public Short getCompetencyLevelLookupId() {
        return competencyLevelLookupId;
    }

    public void setCompetencyLevelLookupId(Short competencyLevelLookupId) {
        this.competencyLevelLookupId = competencyLevelLookupId;
    }

    public String getCompetencyLevelName() {
        return competencyLevelName;
    }

    public void setCompetencyLevelName(String competencyLevelName) {
        this.competencyLevelName = competencyLevelName;
    }

    public Short getCompetencyMinYears() {
        return competencyMinYears;
    }

    public void setCompetencyMinYears(Short competencyMinYears) {
        this.competencyMinYears = competencyMinYears;
    }
}
