import { useEffect } from 'react';
import type { Workout } from '../utils/workoutStore';

interface DeleteConfirmModalProps {
  workout: Workout | null;
  onCancel: () => void;
  onConfirm: () => void;
}

export function DeleteConfirmModal({
  workout,
  onCancel,
  onConfirm,
}: DeleteConfirmModalProps) {
  useEffect(() => {
    if (!workout) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [workout, onCancel]);

  if (!workout) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-workout-title"
    >
      <div className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-800 dark:bg-slate-900">
        <h2
          id="delete-workout-title"
          className="text-lg font-bold text-slate-900 dark:text-slate-100"
        >
          Excluir treino?
        </h2>
        <p className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-slate-300">
          O treino <span className="font-semibold text-slate-900 dark:text-white">{workout.name}</span> será removido permanentemente da sua lista.
        </p>

        <div className="mt-6 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="min-h-[44px] rounded-xl px-4 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900 whitespace-nowrap shrink-0 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="min-h-[44px] rounded-xl bg-rose-600 px-5 py-2 text-sm font-semibold text-white transition-colors hover:bg-rose-700 whitespace-nowrap shrink-0"
          >
            Excluir
          </button>
        </div>
      </div>
    </div>
  );
}
