package mcp.server.domain.missions.model;

import java.util.Objects;

import mcp.server.domain.customers.model.CustomerId;
import mcp.server.domain.shared_kernel.exception.DomainInvariantViolationException;

public final class Mission {

    private final MissionId id;
    private final CustomerId customerId;
    private final String customerName;
    private final String customerEmail;
    private final String title;
    private final MissionAvailability availability;

    public Mission(
            MissionId id,
            CustomerId customerId,
            String customerName,
            String customerEmail,
            String title,
            MissionAvailability availability) {
        if (id == null) {
            throw new DomainInvariantViolationException("Mission id must not be null");
        }
        if (customerId == null || !customerId.isAssigned()) {
            throw new DomainInvariantViolationException("Mission customerId must be assigned");
        }
        if (customerName == null || customerName.isBlank()) {
            throw new DomainInvariantViolationException("Mission customerName must not be blank");
        }
        if (customerEmail == null || !customerEmail.contains("@")) {
            throw new DomainInvariantViolationException("Mission customerEmail must contain @");
        }
        if (title == null || title.isBlank()) {
            throw new DomainInvariantViolationException("Mission title must not be blank");
        }
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName.strip();
        this.customerEmail = customerEmail.strip();
        this.title = title.strip();
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public MissionId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getTitle() {
        return title;
    }

    public MissionAvailability getAvailability() {
        return availability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Mission other))
            return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Mission{id=" + id + ", customerId=" + customerId + ", customerName='" + customerName
                + "', customerEmail='" + customerEmail + "'"
                + ", title='" + title + "', availability=" + availability + "}";
    }
}
