package mcp.server.domain.customers.api;

/**
 * Customer-owned read contract for other domain modules.
 */
public interface CustomerQuery {

    long countRegisteredCustomers();
}
