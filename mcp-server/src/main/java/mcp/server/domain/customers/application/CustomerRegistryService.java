package mcp.server.domain.customers.application;

import org.springframework.stereotype.Service;

import java.util.List;

import mcp.server.domain.customers.exception.CustomerNotFoundException;
import mcp.server.domain.customers.model.Customer;
import mcp.server.domain.customers.model.CustomerId;
import mcp.server.domain.customers.persistence.CustomerRepository;

/**
 * Domain service for Customer aggregate lifecycle operations.
 */
@Service
public class CustomerRegistryService {

    private final CustomerRepository customerRepository;

    public CustomerRegistryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer register(String customerName, String customerEmail) {
        Customer customer = new Customer(new CustomerId(0), customerName, customerEmail);
        return customerRepository.save(customer);
    }

    public Customer resolveOrRegister(String customerName, String customerEmail) {
        String normalizedName = customerName == null ? "" : customerName.strip();
        String normalizedEmail = customerEmail == null ? "" : customerEmail.strip();
        return customerRepository.findByCustomerNameAndCustomerEmail(normalizedName, normalizedEmail)
                .orElseGet(() -> register(normalizedName, normalizedEmail));
    }

    public Customer findById(CustomerId id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    public List<Customer> listAll() {
        return customerRepository.findAll();
    }

    public Customer update(CustomerId id, String customerName, String customerEmail) {
        Customer existing = findById(id);
        Customer updated = new Customer(existing.getId(), customerName, customerEmail);
        return customerRepository.save(updated);
    }

    public void delete(CustomerId id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException(id);
        }
        customerRepository.delete(id);
    }
}
