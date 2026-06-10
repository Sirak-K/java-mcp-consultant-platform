package mcp.server.domain.customers.model;

import java.util.Objects;

import mcp.server.domain.shared_kernel.exception.DomainInvariantViolationException;

public final class Customer {

    private final CustomerId id;
    private final String customerName;
    private final String customerEmail;

    public Customer(CustomerId id, String customerName, String customerEmail) {
        if (id == null) {
            throw new DomainInvariantViolationException("Customer id must not be null");
        }
        if (customerName == null || customerName.isBlank()) {
            throw new DomainInvariantViolationException("Customer name must not be blank");
        }
        if (customerEmail == null || !customerEmail.contains("@")) {
            throw new DomainInvariantViolationException("Customer email must contain @");
        }
        this.id = id;
        this.customerName = customerName.strip();
        this.customerEmail = customerEmail.strip();
    }

    public CustomerId getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Customer other))
            return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", customerName='" + customerName
                + "', customerEmail='" + customerEmail + "'}";
    }
}
