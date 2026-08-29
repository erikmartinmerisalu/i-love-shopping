import { useEffect } from 'react';

type ToastProps = {
  message: string;
  onDismiss: () => void;
  durationMs?: number;
};

export default function Toast({ message, onDismiss, durationMs = 3500 }: ToastProps) {
  useEffect(() => {
    if (!message) {
      return;
    }
    const id = window.setTimeout(onDismiss, durationMs);
    return () => window.clearTimeout(id);
  }, [message, durationMs, onDismiss]);

  if (!message) {
    return null;
  }

  return (
    <div
      className="fixed top-24 left-1/2 z-50 flex -translate-x-1/2 items-center gap-3 rounded-xl border border-green-400/30 bg-gray-900 px-4 py-3 text-sm text-white shadow-lg"
      role="status"
      aria-live="polite"
    >
      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-green-500/30 bg-green-500/20 text-sm font-bold text-green-400">
        ✓
      </span>
      <span className="font-medium">{message}</span>
    </div>
  );
}
