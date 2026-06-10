package mcp.server.foundation.tool_interface;

import mcp.server.foundation.rpc.RPCErr;
import mcp.server.foundation.rpc.RPCMappedExcep;
import mcp.server.foundation.rpc.error.ExcepClassifSupport;

import java.time.format.DateTimeParseException;

final class ToolInvocExcepMapper {

  private ToolInvocExcepMapper() {
  }

  static RPCMappedExcep map(Throwable throwable) {

    if (throwable instanceof RPCMappedExcep mapped) {
      return mapped;
    }

    Throwable rootCause = ExcepClassifSupport.ExceptionClassifSupportRootCauseOf(throwable);

    if (rootCause instanceof RPCMappedExcep mapped) {
      return mapped;
    }

    if (isConcurrConflict(throwable, rootCause)) {
      return RPCMappedExcep.RPCMappedExcepBusinessRuleViolation("Concurrency conflict");
    }

    if (isUniquenessViolation(throwable, rootCause)) {
      return RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "Operation violates uniqueness constraints");
    }

    if (isRelationalIntegrityViolation(throwable, rootCause)) {
      return RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(
          "Operation violates relational integrity");
    }

    if (ExcepClassifSupport.ExceptionClassifSupportIsDomainFailure(rootCause)) {
      return RPCMappedExcep.RPCMappedExcepBusinessRuleViolation(rootCause.getMessage());
    }

    if (rootCause instanceof DateTimeParseException) {
      return new RPCMappedExcep(RPCErr.RPCErrInvalidParams("Invalid date format"));
    }

    if (rootCause instanceof ToolExecCancelledExcep
        || rootCause instanceof InterruptedException) {
      return RPCMappedExcep.RPCMappedExcepBusinessRuleViolation("Tool execution cancelled");
    }

    if (rootCause instanceof IllegalArgumentException illegalArgumentException) {
      return new RPCMappedExcep(
          RPCErr.RPCErrInvalidParams(normalizeInvalidParamsMessage(illegalArgumentException.getMessage())));
    }

    String message = rootCause != null && rootCause.getMessage() != null
        ? rootCause.getMessage()
        : throwable.getMessage();

    return new RPCMappedExcep(RPCErr.RPCErrInternalErr(message));
  }

  private static boolean isConcurrConflict(Throwable throwable, Throwable rootCause) {
    return ExcepClassifSupport.ExceptionClassifSupportIsConcurrConflict(throwable)
        || ExcepClassifSupport.ExceptionClassifSupportIsConcurrConflict(rootCause);
  }

  private static boolean isUniquenessViolation(Throwable throwable, Throwable rootCause) {

    if (!isDataIntegrityThrowable(throwable, rootCause)) {
      return false;
    }

    String message = lowerCaseMessage(rootCause);
    return message.contains("duplicate")
        || message.contains("unique")
        || message.contains("uk_");
  }

  private static boolean isRelationalIntegrityViolation(Throwable throwable, Throwable rootCause) {

    if (!isDataIntegrityThrowable(throwable, rootCause)) {
      return false;
    }

    String message = lowerCaseMessage(rootCause);
    return message.contains("foreign key")
        || message.contains("a foreign key constraint fails")
        || message.contains("cannot delete or update a parent row");
  }

  private static String normalizeInvalidParamsMessage(String message) {

    if (message == null || message.isBlank()) {
      return "Invalid parameters";
    }

    if (message.startsWith("No enum constant ")) {
      return "Invalid enum value";
    }

    return message;
  }

  private static String lowerCaseMessage(Throwable throwable) {
    return ExcepClassifSupport.ExceptionClassifSupportLowerCaseMessage(throwable);
  }

  private static boolean isDataIntegrityThrowable(Throwable throwable, Throwable rootCause) {
    return ExcepClassifSupport.ExceptionClassifSupportIsDataIntegrityThrowable(throwable)
        || ExcepClassifSupport.ExceptionClassifSupportIsDataIntegrityThrowable(rootCause);
  }
}
