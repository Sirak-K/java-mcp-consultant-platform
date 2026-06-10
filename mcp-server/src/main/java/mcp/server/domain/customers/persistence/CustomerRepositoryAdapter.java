package mcp.server.domain.customers.persistence;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import mcp.server.domain.customers.model.Customer;
import mcp.server.domain.customers.model.CustomerId;

/**
 * Persistence adapter implementing {@link CustomerRepository} via JPA.
 *
 * <p>
 * Translates between the {@link Customer} domain aggregate and the
 * {@link CustomerEntity} JPA entity. No persistence type leaks through
 * the implemented interface.
 */
@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepo;

    public CustomerRepositoryAdapter(CustomerJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = toEntity(customer);
        CustomerEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpaRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Customer> findByCustomerNameAndCustomerEmail(String customerName, String customerEmail) {
        return jpaRepo.findByCustomerNameAndCustomerEmail(customerName, customerEmail).map(this::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return jpaRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return jpaRepo.count();
    }

    @Override
    public void delete(CustomerId id) {
        jpaRepo.deleteById(id.value());
    }

    @Override
    public boolean existsById(CustomerId id) {
        return jpaRepo.existsById(id.value());
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private CustomerEntity toEntity(Customer domain) {
        Long entityId = domain.getId().value() == 0 ? null : domain.getId().value();
        return new CustomerEntity(
                entityId,
                domain.getCustomerName(),
                domain.getCustomerEmail());
    }

    private Customer toDomain(CustomerEntity entity) {
        return new Customer(
                new CustomerId(entity.getId()),
                entity.getCustomerName(),
                entity.getCustomerEmail());
    }
}
