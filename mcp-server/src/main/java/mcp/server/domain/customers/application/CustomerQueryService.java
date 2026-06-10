package mcp.server.domain.customers.application;

import org.springframework.stereotype.Service;

import mcp.server.domain.customers.api.CustomerQuery;
import mcp.server.domain.customers.persistence.CustomerRepository;

/**
 * Read-only customer query facade for cross-domain consumers.
 */
@Service
public class CustomerQueryService implements CustomerQuery {

    private final CustomerRepository customerRepository;

    public CustomerQueryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public long countRegisteredCustomers() {
        return customerRepository.count();
    }
}
