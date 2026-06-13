package mcp.server.domain.reference_data.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "skills_primary", schema = "consultant_platform")
public class PrimarySkillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "primary_skill_id")
    private Long id;

    @Column(name = "skill_title", nullable = false, length = 150, unique = true)
    private String skillTitle;

    protected PrimarySkillEntity() {
    }

    public PrimarySkillEntity(Long id, String skillTitle) {
        this.id = id;
        this.skillTitle = skillTitle;
    }

    public Long getId() {
        return id;
    }

    public String getSkillTitle() {
        return skillTitle;
    }
}
