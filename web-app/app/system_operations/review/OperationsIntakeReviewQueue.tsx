import { useEffect, useState, type ReactNode } from "react";
import { ErrorPlaceholder, LoadingPlaceholder } from "~/shared/components";
import type { ApiError } from "~/shared/api/apiErrors";

interface ReviewableItem {
  outcome: string;
  createdAt: string;
}

interface ReviewItemStatusView {
  label: string;
  className?: string;
}

interface ReviewItemDeleteAction<TItem> {
  deleteItem: (id: number) => Promise<void>;
  getConfirmationMessage: (item: TItem) => string;
  errorMessage?: string;
}

interface OperationsIntakeReviewQueueProps<
  TItem extends ReviewableItem,
  TEditInput,
> {
  title: string;
  description: string;
  contract: string[];
  loadItems: () => Promise<TItem[]>;
  getItemId: (item: TItem) => number;
  getItemTitle: (item: TItem) => string;
  getItemStatusView?: (item: TItem) => ReviewItemStatusView | null;
  isItemReviewComplete?: (item: TItem) => boolean;
  deleteAction?: ReviewItemDeleteAction<TItem>;
  editItem?: (id: number, input: TEditInput) => Promise<TItem>;
  approveItem?: (id: number) => Promise<TItem>;
  rejectItem?: (id: number) => Promise<TItem>;
  buildEditInput?: (
    item: TItem,
    outcome: string,
    editWorkingCopy: unknown,
  ) => TEditInput;
  buildEditWorkingCopy?: (item: TItem) => unknown;
  contentClassName?: string;
  renderEditFields?: (
    item: TItem,
    editWorkingCopy: unknown,
    updateEditWorkingCopy: (updater: (current: unknown) => unknown) => void,
  ) => ReactNode;
  renderSummary: (item: TItem) => ReactNode;
  renderMatches: (item: TItem) => ReactNode;
}

export function OperationsIntakeReviewQueue<
  TItem extends ReviewableItem,
  TEditInput,
>({
  title,
  description,
  contract,
  loadItems,
  getItemId,
  getItemTitle,
  getItemStatusView,
  isItemReviewComplete,
  deleteAction,
  editItem,
  approveItem,
  rejectItem,
  buildEditInput,
  buildEditWorkingCopy,
  contentClassName,
  renderEditFields,
  renderSummary,
  renderMatches,
}: OperationsIntakeReviewQueueProps<TItem, TEditInput>) {
  const [items, setItems] = useState<TItem[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeOutcome, setActiveOutcome] = useState<Record<number, string>>(
    {},
  );
  const [activeEditWorkingCopys, setActiveEditWorkingCopys] = useState<
    Record<number, unknown>
  >({});
  const [activeEditMode, setActiveEditMode] = useState<Record<number, boolean>>(
    {},
  );
  const [busyId, setBusyId] = useState<number | null>(null);
  const [completedApproveIds, setCompletedApproveIds] = useState<
    Record<number, boolean>
  >({});

  useEffect(() => {
    let mounted = true;
    loadItems()
      .then((result) => {
        if (mounted) {
          setItems(result);
          setError(null);
        }
      })
      .catch((err: ApiError) => {
        if (mounted) {
          setError(err.message ?? "Kunde inte ladda granskningskön.");
        }
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, [loadItems]);

  function replaceItem(updated: TItem) {
    const updatedId = getItemId(updated);
    setItems(
      (current) =>
        current?.map((item) =>
          getItemId(item) === updatedId ? updated : item,
        ) ?? [updated],
    );
    if (buildEditWorkingCopy) {
      setActiveEditWorkingCopys((current) => ({
        ...current,
        [updatedId]: buildEditWorkingCopy(updated),
      }));
    }
  }

  async function submitEdit(item: TItem) {
    if (!editItem || !buildEditInput) {
      return;
    }
    const itemId = getItemId(item);
    setBusyId(itemId);
    setError(null);
    const editWorkingCopy =
      activeEditWorkingCopys[itemId] ?? buildEditWorkingCopy?.(item) ?? null;

    try {
      const updated = await editItem(
        itemId,
        buildEditInput(
          item,
          activeOutcome[itemId] ?? item.outcome,
          editWorkingCopy,
        ),
      );
      replaceItem(updated);
      setActiveEditMode((current) => ({ ...current, [itemId]: false }));
    } catch (err) {
      const apiError = err as ApiError;
      setError(apiError.message ?? "Kunde inte spara granskningen.");
    } finally {
      setBusyId(null);
    }
  }

  async function submitDelete(item: TItem) {
    if (!deleteAction) {
      return;
    }
    const itemId = getItemId(item);
    const shouldDelete = window.confirm(deleteAction.getConfirmationMessage(item));
    if (!shouldDelete) {
      return;
    }

    setBusyId(itemId);
    setError(null);
    try {
      await deleteAction.deleteItem(itemId);
      setItems(
        (current) =>
          current?.filter((currentItem) => getItemId(currentItem) !== itemId) ??
          [],
      );
      setActiveOutcome((current) => {
        const next = { ...current };
        delete next[itemId];
        return next;
      });
      setActiveEditWorkingCopys((current) => {
        const next = { ...current };
        delete next[itemId];
        return next;
      });
      setActiveEditMode((current) => {
        const next = { ...current };
        delete next[itemId];
        return next;
      });
    } catch (err) {
      const apiError = err as ApiError;
      setError(
        apiError.message ??
          deleteAction.errorMessage ??
          "Kunde inte ta bort underlaget.",
      );
    } finally {
      setBusyId(null);
    }
  }

  async function submitStatus(id: number, action: "approve" | "reject") {
    if (!approveItem || !rejectItem) {
      return;
    }
    if (action === "approve") {
      setCompletedApproveIds((current) => ({ ...current, [id]: true }));
      setActiveEditMode((current) => ({ ...current, [id]: false }));
    }
    setBusyId(id);
    setError(null);

    try {
      const updated =
        action === "approve" ? await approveItem(id) : await rejectItem(id);
      replaceItem(updated);
    } catch (err) {
      if (action === "approve") {
        setCompletedApproveIds((current) => {
          const next = { ...current };
          delete next[id];
          return next;
        });
      }
      const apiError = err as ApiError;
      setError(apiError.message ?? "Kunde inte spara granskningsbeslutet.");
    } finally {
      setBusyId(null);
    }
  }

  if (loading) return <LoadingPlaceholder />;

  return (
    <div className={["p-6", contentClassName].filter(Boolean).join(" ")}>
      <header className="mb-6 max-w-3xl">
        <h1 className="text-title text-2xl font-semibold">{title}</h1>
        <p className="text-muted mt-2 text-sm leading-6">{description}</p>
      </header>

      {error && (
        <div className="mb-6">
          <ErrorPlaceholder message={error} />
          <div className="panel mt-4 rounded p-4 text-sm">
            <h2 className="text-section mb-2 font-medium">
              Förväntat API-kontrakt
            </h2>
            <ul className="text-muted grid gap-1">
              {contract.map((line) => (
                <li key={line}>{line}</li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {!items || items.length === 0 ? (
        <div className="panel rounded p-6 text-sm">
          <p className="text-soft">Inga underlag väntar på granskning.</p>
        </div>
      ) : (
        <div className="grid gap-4">
          {items.map((item) => {
            const itemId = getItemId(item);
            const editWorkingCopy =
              activeEditWorkingCopys[itemId] ??
              buildEditWorkingCopy?.(item) ??
              null;
            const savedEditWorkingCopy = buildEditWorkingCopy?.(item) ?? null;
            const hasCustomEditMode = Boolean(renderEditFields);
            const canEdit = Boolean(
              editItem && buildEditInput && renderEditFields,
            );
            const canDelete = Boolean(deleteAction);
            const canChangeStatus = Boolean(approveItem && rejectItem);
            const itemReviewComplete = isItemReviewComplete?.(item) ?? false;
            const itemStatusView = getItemStatusView?.(item) ?? null;
            const isApproveFading = completedApproveIds[itemId] === true;
            const canEditCurrentItem = canEdit && !itemReviewComplete;
            const canChangeCurrentStatus =
              canChangeStatus && !itemReviewComplete;
            const isEditing = canEdit && activeEditMode[itemId] === true;
            const activeOutcomeValue = activeOutcome[itemId] ?? item.outcome;
            const editWorkingCopyChanged =
              JSON.stringify(editWorkingCopy) !==
              JSON.stringify(savedEditWorkingCopy);
            const outcomeChanged = activeOutcomeValue !== item.outcome;
            const hasUnsavedEdits = editWorkingCopyChanged || outcomeChanged;
            const updateEditWorkingCopy = (
              updater: (current: unknown) => unknown,
            ) => {
              setActiveEditWorkingCopys((current) => ({
                ...current,
                [itemId]: updater(current[itemId] ?? editWorkingCopy),
              }));
            };
            const openEditMode = () => {
              setActiveEditWorkingCopys((current) => ({
                ...current,
                [itemId]: current[itemId] ?? savedEditWorkingCopy,
              }));
              setActiveOutcome((current) => ({
                ...current,
                [itemId]: current[itemId] ?? item.outcome,
              }));
              setActiveEditMode((current) => ({ ...current, [itemId]: true }));
            };
            const exitEditMode = () => {
              if (hasUnsavedEdits) {
                const shouldDiscard = window.confirm(
                  "Det finns osparade ändringar. Tryck OK för att lämna redigeringsläge utan att spara, eller Avbryt för att fortsätta redigera.",
                );
                if (shouldDiscard) {
                  setActiveEditWorkingCopys((current) => ({
                    ...current,
                    [itemId]: savedEditWorkingCopy,
                  }));
                  setActiveOutcome((current) => ({
                    ...current,
                    [itemId]: item.outcome,
                  }));
                  setActiveEditMode((current) => ({
                    ...current,
                    [itemId]: false,
                  }));
                }
                return;
              }
              setActiveEditMode((current) => ({ ...current, [itemId]: false }));
            };

            return (
              <article
                key={itemId}
                className={`panel rounded-2xl p-5 transition-colors duration-700 ${
                  itemReviewComplete ? "review-card-approved" : ""
                }`}
              >
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <h2 className="text-title mt-2 text-xl font-semibold">
                      {getItemTitle(item)}
                    </h2>
                    <p className="text-soft mt-1 text-xs">
                      Inskickat: {item.createdAt}
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    {itemStatusView && (
                      <span
                        className={[
                          "chip rounded-full px-3 py-1 text-xs font-medium",
                          itemStatusView.className,
                        ]
                          .filter(Boolean)
                          .join(" ")}
                      >
                        {itemStatusView.label}
                      </span>
                    )}
                    {canDelete && (
                      <button
                        type="button"
                        disabled={busyId === itemId}
                        className="btn btn-danger rounded px-4 py-2 text-sm font-semibold"
                        onClick={() => void submitDelete(item)}
                      >
                        Ta bort
                      </button>
                    )}
                  </div>
                </div>

                {!hasCustomEditMode || !isEditing ? (
                  <>
                    <div className="text-muted mt-4 text-sm leading-6">
                      {renderSummary(item)}
                    </div>
                    <div className="mt-4">{renderMatches(item)}</div>
                  </>
                ) : null}
                {renderEditFields && isEditing && (
                  <div className="mt-4">
                    {renderEditFields(
                      item,
                      editWorkingCopy,
                      updateEditWorkingCopy,
                    )}
                  </div>
                )}

                {isEditing ? (
                  <label className="mt-4 grid gap-2 text-sm">
                    <span className="text-label font-medium">
                      Beslut / ändringskommentar
                    </span>
                    <textarea
                      className="input min-h-20 rounded px-3 py-2"
                      value={activeOutcomeValue}
                      onChange={(event) =>
                        setActiveOutcome((current) => ({
                          ...current,
                          [itemId]: event.target.value,
                        }))
                      }
                      placeholder="Skriv OPS-bedömning eller edit-kommentar."
                    />
                  </label>
                ) : item.outcome ? (
                  <div className="panel-soft mt-4 rounded p-4 text-sm">
                    <p className="text-label mb-1 font-medium">Beslut</p>
                    <p className="text-muted whitespace-pre-wrap">
                      {item.outcome}
                    </p>
                  </div>
                ) : null}

                {(canEdit || canChangeStatus) && (
                  <div
                    className={`mt-4 flex flex-wrap gap-2 transition-opacity duration-1000 ${
                      itemReviewComplete || isApproveFading
                        ? "pointer-events-none opacity-0"
                        : "opacity-100"
                    }`}
                    aria-hidden={itemReviewComplete || isApproveFading}
                  >
                    {isEditing ? (
                      <>
                        <button
                          type="button"
                          disabled={busyId === itemId || !hasUnsavedEdits}
                          className="btn btn-main rounded px-4 py-2 text-sm font-medium disabled:opacity-50"
                          onClick={() => void submitEdit(item)}
                        >
                          Spara ändringar
                        </button>
                        {hasCustomEditMode && (
                          <button
                            type="button"
                            disabled={busyId === itemId}
                            className="btn btn-soft rounded px-4 py-2 text-sm font-medium"
                            onClick={exitEditMode}
                          >
                            Lämna redigeringsläge
                          </button>
                        )}
                      </>
                    ) : (
                      <>
                        {canEditCurrentItem && (
                          <button
                            type="button"
                            disabled={busyId === itemId}
                            className="btn btn-soft rounded px-4 py-2 text-sm font-medium"
                            onClick={openEditMode}
                            title="Redigeringsläge"
                            aria-label="Redigeringsläge"
                          >
                            <svg
                              aria-hidden="true"
                              className="mr-2 inline h-4 w-4"
                              viewBox="0 0 20 20"
                              fill="currentColor"
                            >
                              <path d="M13.6 2.6a2 2 0 0 1 2.8 2.8l-8.9 8.9-3.7.9.9-3.7 8.9-8.9Z" />
                              <path d="M3 17h14v1.5H3V17Z" />
                            </svg>
                            Redigeringsläge
                          </button>
                        )}
                        {canChangeCurrentStatus && (
                          <>
                            <button
                              type="button"
                              disabled={busyId === itemId}
                              className="btn btn-main rounded px-4 py-2 text-sm font-medium"
                              onClick={() =>
                                void submitStatus(itemId, "approve")
                              }
                            >
                              Godkänn
                            </button>
                            <button
                              type="button"
                              disabled={busyId === itemId}
                              className="btn btn-danger rounded px-4 py-2 text-sm font-medium"
                              onClick={() =>
                                void submitStatus(itemId, "reject")
                              }
                            >
                              Avslå
                            </button>
                          </>
                        )}
                      </>
                    )}
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
