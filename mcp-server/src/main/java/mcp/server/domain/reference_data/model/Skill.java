package mcp.server.domain.reference_data.model;

import java.util.Objects;

import mcp.server.domain.shared_kernel.exception.DomainInvariantViolationException;

public final class Skill {

    private final SkillId id;
    private final String title;

    public Skill(SkillId id, String title) {
        if (id == null) {
            throw new DomainInvariantViolationException("Skill id must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new DomainInvariantViolationException("Skill title must not be blank");
        }
        this.id = id;
        this.title = title.strip();
    }

    public SkillId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Skill other))
            return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Skill{id=" + id + ", title='" + title + "'}";
    }
}
