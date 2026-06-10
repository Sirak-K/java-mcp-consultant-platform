package mcp.server.domain.customers.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CustomerEntity}.
 *
 * <p>
 * Used exclusively by {@code CustomerRepositoryAdapter} to implement the
 * domain contract {@code CustomerRepository}. Must not be injected outside
 * the customer persistence package.
 */
public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByCustomerNameAndCustomerEmail(String customerName, String customerEmail);
}
