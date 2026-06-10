package mcp.server.domain.reference_data.model;

import java.util.Objects;

import mcp.server.domain.shared_kernel.exception.DomainInvariantViolationException;

public final class Role {

    private final RoleId id;
    private final String title;

    public Role(RoleId id, String title) {
        if (id == null) {
            throw new DomainInvariantViolationException("Role id must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new DomainInvariantViolationException("Role title must not be blank");
        }
        this.id = id;
        this.title = title.strip();
    }

    public RoleId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Role other))
            return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{id=" + id + ", title='" + title + "'}";
    }
}
