package mcp.server.domain.shared_kernel.validation;

/**
 * Thrown when application input cannot be accepted by a domain use case.
 */
public final class InvalidApplicationInputException extends IllegalArgumentException {

    public InvalidApplicationInputException(String message) {
        super(message);
    }
}
