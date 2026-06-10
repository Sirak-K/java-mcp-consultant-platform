export function LoadingPlaceholder() {
  return <div className="text-placeholder p-6 text-sm">Laddar...</div>;
}

export function ErrorPlaceholder({ message }: { message: string }) {
  return <div className="text-danger p-6 text-sm">{message}</div>;
}
