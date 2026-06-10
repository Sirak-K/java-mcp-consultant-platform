package mcp.server.foundation.rpc.error;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Locale;

public final class ExcepClassifSupport {

  private ExcepClassifSupport() {
  }

  public static Throwable ExceptionClassifSupportRootCauseOf(Throwable throwable) {
    Throwable cursor = throwable;

    while (cursor != null && cursor.getCause() != null && cursor.getCause() != cursor) {
      cursor = cursor.getCause();
    }

    return cursor;
  }

  public static boolean ExceptionClassifSupportIsNotFound(Throwable throwable) {
    return ExceptionClassifSupportIsSimpleNameOrSubtype(throwable, "EntityNotFoundException")
        || ExceptionClassifSupportSimpleNameEndsWith(throwable, "NotFoundException");
  }

  public static boolean ExceptionClassifSupportIsDomainInvariantViolation(Throwable throwable) {
    return ExceptionClassifSupportIsSimpleNameOrSubtype(throwable, "DomainInvariantViolationException");
  }

  public static boolean ExceptionClassifSupportIsDomainFailure(Throwable throwable) {
    return ExceptionClassifSupportIsSimpleNameOrSubtype(throwable, "DomainException");
  }

  public static boolean ExceptionClassifSupportIsConcurrConflict(Throwable throwable) {
    return ExceptionClassifSupportIsTypeOrSubtype(
            throwable, "org.springframework.orm.ObjectOptimisticLockingFailureException")
        || ExceptionClassifSupportIsTypeOrSubtype(throwable, "jakarta.persistence.OptimisticLockException")
        || ExceptionClassifSupportIsTypeOrSubtype(throwable, "org.hibernate.StaleObjectStateException");
  }

  public static boolean ExceptionClassifSupportIsDataIntegrityThrowable(Throwable throwable) {
    return ExceptionClassifSupportIsTypeOrSubtype(throwable, "org.springframework.dao.DataIntegrityViolationException")
        || ExceptionClassifSupportIsTypeOrSubtype(throwable, "org.hibernate.exception.ConstraintViolationException")
        || throwable instanceof SQLIntegrityConstraintViolationException;
  }

  public static String ExceptionClassifSupportLowerCaseMessage(Throwable throwable) {
    return throwable != null && throwable.getMessage() != null
        ? throwable.getMessage().toLowerCase(Locale.ROOT)
        : "";
  }

  public static boolean ExceptionClassifSupportIsTypeOrSubtype(Throwable throwable, String fqcn) {
    if (throwable == null) {
      return false;
    }

    Class<?> cursor = throwable.getClass();
    while (cursor != null) {
      if (fqcn.equals(cursor.getName())) {
        return true;
      }
      cursor = cursor.getSuperclass();
    }

    return false;
  }

  private static boolean ExceptionClassifSupportIsSimpleNameOrSubtype(
      Throwable throwable,
      String simpleName) {

    if (throwable == null) {
      return false;
    }

    Class<?> cursor = throwable.getClass();
    while (cursor != null) {
      if (simpleName.equals(cursor.getSimpleName())) {
        return true;
      }
      cursor = cursor.getSuperclass();
    }

    return false;
  }

  private static boolean ExceptionClassifSupportSimpleNameEndsWith(
      Throwable throwable,
      String suffix) {

    return throwable != null
        && throwable.getClass().getSimpleName().endsWith(suffix);
  }
}
