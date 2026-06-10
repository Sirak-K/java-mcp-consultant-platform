package mcp.server.domain.customers.persistence;

import java.util.List;
import java.util.Optional;

import mcp.server.domain.customers.model.Customer;
import mcp.server.domain.customers.model.CustomerId;

/**
 * Stable domain contract for Customer aggregate persistence.
 *
 * <p>
 * This interface is the only permitted boundary through which Customer
 * aggregates may cross the persistence layer. No persistence-layer type may
 * leak through this contract.
 *
 * <p>
 * Model types ({@link Customer}, {@link CustomerId}) are
 * realized by the core domain model.
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);

    Optional<Customer> findByCustomerNameAndCustomerEmail(String customerName, String customerEmail);

    List<Customer> findAll();

    long count();

    void delete(CustomerId id);

    boolean existsById(CustomerId id);
}
