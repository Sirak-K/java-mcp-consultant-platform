package mcp.server.foundation.rpc.error;

import mcp.server.foundation.rpc.RPCErr;
import mcp.server.foundation.rpc.RPCMappedExcep;

import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

/**
 * Maps exceptions to internal observability error types.
 */
public final class ErrClassifier {

  public ErrType ErrClassifierClassify(Throwable throwable) {
    Objects.requireNonNull(throwable, "throwable");

    Throwable rootCause = ExcepClassifSupport.ExceptionClassifSupportRootCauseOf(throwable);

    if (rootCause instanceof RPCMappedExcep mappedException) {
      return classifyRPCMappedExcep(mappedException);
    }

    if (ExcepClassifSupport.ExceptionClassifSupportIsNotFound(rootCause)) {
      return ErrType.NOT_FOUND;
    }

    if (ErrClassifierIsConcurrConflict(rootCause)) {
      return ErrType.CONFLICT;
    }

    if (ErrClassifierIsDataIntegrityConflict(rootCause)) {
      return ErrType.DATA_INTEGRITY_ERROR;
    }

    if (rootCause instanceof DateTimeParseException
        || rootCause instanceof IllegalArgumentException) {
      return ErrType.VALIDATION_ERROR;
    }

    if (ExcepClassifSupport.ExceptionClassifSupportIsDomainInvariantViolation(rootCause)) {
      return ErrType.DOMAIN_ERROR;
    }

    if (ExcepClassifSupport.ExceptionClassifSupportIsDomainFailure(rootCause)) {
      return ErrType.DOMAIN_ERROR;
    }

    return ErrType.INTERNAL_ERROR;
  }

  private ErrType classifyRPCMappedExcep(RPCMappedExcep mappedException) {

    RPCErr error = mappedException.RPCMappedExcepGetRPCErr();
    if (error == null) {
      return ErrType.INTERNAL_ERROR;
    }

    return switch (error.RPCErrGetCode()) {
      case RPCErr.CODE_PARSE_ERROR,
          RPCErr.CODE_INVALID_REQUEST,
          RPCErr.CODE_METHOD_NOT_FOUND ->
        ErrType.RPC_PROTOCOL_ERROR;
      case RPCErr.CODE_INVALID_PARAMS -> ErrType.VALIDATION_ERROR;
      case RPCErr.CODE_INTERNAL_ERROR -> ErrType.INTERNAL_ERROR;
      case RPCErr.CODE_BUSINESS_RULE_VIOLATION -> classifyBusinessRuleMessage(error.RPCErrGetMessage());
      default -> ErrType.INTERNAL_ERROR;
    };
  }

  private ErrType classifyBusinessRuleMessage(String message) {

    String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);

    if (normalized.contains("not initialized")
        || normalized.contains("throttle")) {
      return ErrType.THROTTLE_REJECTED;
    }

    if (normalized.contains("not found")) {
      return ErrType.NOT_FOUND;
    }

    if (normalized.contains("concurrency")
        || normalized.contains("duplicate")
        || normalized.contains("unique")) {
      return ErrType.CONFLICT;
    }

    return ErrType.DOMAIN_ERROR;
  }

  private static boolean ErrClassifierIsConcurrConflict(Throwable throwable) {
    return ExcepClassifSupport.ExceptionClassifSupportIsConcurrConflict(throwable);
  }

  private static boolean ErrClassifierIsDataIntegrityConflict(Throwable throwable) {

    if (!ExcepClassifSupport.ExceptionClassifSupportIsDataIntegrityThrowable(throwable)) {
      return false;
    }

    String message = ExcepClassifSupport.ExceptionClassifSupportLowerCaseMessage(throwable);

    return message.contains("foreign key")
        || message.contains("constraint")
        || message.contains("cannot delete or update a parent row");
  }
}
