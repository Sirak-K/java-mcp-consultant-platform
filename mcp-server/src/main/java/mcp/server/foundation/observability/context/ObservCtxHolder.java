package mcp.server.foundation.observability.context;

/**
 * Thread-local holder for transient observation context.
 */
public final class ObservCtxHolder {

  private static final ThreadLocal<ObservCtx> CURRENT = new ThreadLocal<>();

  private ObservCtxHolder() {
  }

  public static void ObservCtxHolderSet(ObservCtx context) {
    if (context == null) {
      CURRENT.remove();
      return;
    }

    CURRENT.set(context);
  }

  public static ObservCtx ObservCtxHolderGet() {
    return CURRENT.get();
  }

  public static void ObservCtxHolderClear() {
    CURRENT.remove();
  }

  public static Scope ObservCtxHolderOpenScope(ObservCtx context) {
    ObservCtx previous = ObservCtxHolderGet();
    ObservCtxHolderSet(context);
    return new Scope(previous);
  }

  public static void ObservCtxHolderRestore(ObservCtx previousContext) {
    if (previousContext == null) {
      ObservCtxHolderClear();
      return;
    }

    ObservCtxHolderSet(previousContext);
  }

  public static final class Scope implements AutoCloseable {

    private final ObservCtx previousContext;
    private boolean closed;

    private Scope(ObservCtx previousContext) {
      this.previousContext = previousContext;
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }

      closed = true;
      ObservCtxHolderRestore(previousContext);
    }
  }
}
