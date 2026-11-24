import { useEffect, useRef, type PointerEvent as ReactPointerEvent } from 'react';
import { Check, ChevronRight, Pencil, Trash2, X } from 'lucide-react';
import { ACCENT_PALETTES } from '../utils/themePalettes';
import {
  formatRelativeDay,
  hasExceededPointerThreshold,
  type Workout,
} from '../utils/workoutStore';

const LONG_PRESS_MS = 450;

interface WorkoutCardProps {
  workout: Workout;
  isDark: boolean;
  isSelected: boolean;
  onSelectForActions: (workout: Workout) => void;
  onClearSelection: () => void;
  onOpen: (workout: Workout) => void;
  onEdit: (workout: Workout) => void;
  onDelete: (workout: Workout) => void;
}

export function WorkoutCard({
  workout,
  isDark,
  isSelected,
  onSelectForActions,
  onClearSelection,
  onOpen,
  onEdit,
  onDelete,
}: WorkoutCardProps) {
  const palette = ACCENT_PALETTES[workout.colorKey] ?? ACCENT_PALETTES.emerald;
  const accentColor = isDark ? palette.dark.accent : palette.light.accent;
  const accentSoft = isDark ? palette.dark.accentSoft : palette.light.accentSoft;

  const timerRef = useRef<number | null>(null);
  const startPointRef = useRef<{ x: number; y: number } | null>(null);
  const longPressTriggeredRef = useRef(false);

  const clearTimer = () => {
    if (timerRef.current !== null) {
      window.clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    startPointRef.current = null;
  };

  useEffect(() => {
    return () => clearTimer();
  }, []);

  const handlePointerDown = (e: ReactPointerEvent<HTMLButtonElement>) => {
    if (e.pointerType === 'mouse' && e.button !== 0) return;
    clearTimer();
    longPressTriggeredRef.current = false;
    startPointRef.current = { x: e.clientX, y: e.clientY };

    timerRef.current = window.setTimeout(() => {
      longPressTriggeredRef.current = true;
      timerRef.current = null;
      if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
        navigator.vibrate(12);
      }
      onSelectForActions(workout);
    }, LONG_PRESS_MS);
  };

  const handlePointerMove = (e: ReactPointerEvent<HTMLButtonElement>) => {
    if (!startPointRef.current) return;
    if (
      hasExceededPointerThreshold(startPointRef.current, {
        x: e.clientX,
        y: e.clientY,
      })
    ) {
      clearTimer();
    }
  };

  const handleCardClick = () => {
    if (longPressTriggeredRef.current) {
      longPressTriggeredRef.current = false;
      return;
    }
    if (isSelected) {
      onClearSelection();
      return;
    }
    onOpen(workout);
  };

  return (
    <article
      data-testid="workout-card"
      data-selected={isSelected ? 'true' : 'false'}
      onClick={(e) => e.stopPropagation()}
      className={`group relative flex items-center justify-between gap-3 rounded-2xl border px-5 py-4 transition-all duration-150 ${
        isSelected
          ? 'border-[var(--accent)] bg-white shadow-sm ring-2 ring-[var(--accent-ring)] dark:bg-slate-900'
          : 'border-slate-200 bg-white shadow-xs hover:border-slate-300 hover:shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:hover:border-slate-700'
      }`}
    >
      <button
        type="button"
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={clearTimer}
        onPointerCancel={clearTimer}
        onContextMenu={(e) => {
          if (window.innerWidth < 1024) {
            e.preventDefault();
          }
        }}
        onClick={handleCardClick}
        aria-label={isSelected ? `Desmarcar ${workout.name}` : `Abrir ${workout.name}`}
        className="flex min-h-[48px] min-w-0 flex-1 select-none items-center gap-3.5 text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--accent)]"
      >
        <span
          aria-hidden="true"
          style={
            isSelected
              ? { backgroundColor: 'var(--accent)', color: 'var(--accent-fg)' }
              : { backgroundColor: accentSoft, color: accentColor }
          }
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-sm font-bold tracking-tight transition-colors duration-150"
        >
          {isSelected ? (
            <Check className="h-5 w-5" />
          ) : (
            workout.name.replace(/^treino\s*/i, '').slice(0, 2).toUpperCase() || 'TR'
          )}
        </span>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-1.5">
            <h2 className="truncate text-base font-semibold text-slate-900 dark:text-slate-100 sm:text-lg">
              {workout.name}
            </h2>
            {!isSelected && (
              <ChevronRight
                aria-hidden="true"
                className="h-4 w-4 shrink-0 text-slate-400 opacity-0 transition-opacity duration-150 group-hover:opacity-100 dark:text-slate-500"
              />
            )}
          </div>
          <p className="mt-0.5 line-clamp-2 text-xs text-slate-500 dark:text-slate-400 sm:text-sm">
            {workout.focus || `Atualizado: ${formatRelativeDay(workout.updatedAt)}`}
          </p>
        </div>
      </button>

      <div
        className={`shrink-0 items-center ${
          isSelected ? 'flex' : 'hidden lg:flex'
        }`}
      >
        <button
          type="button"
          onClick={() => onEdit(workout)}
          aria-label={`Editar ${workout.name}`}
          className="flex h-11 w-11 items-center justify-center rounded-xl text-slate-500 transition-colors duration-150 hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--accent)] dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
        >
          <Pencil className="h-4 w-4" aria-hidden="true" />
        </button>
        <button
          type="button"
          onClick={() => onDelete(workout)}
          aria-label={`Excluir ${workout.name}`}
          className="flex h-11 w-11 items-center justify-center rounded-xl text-slate-500 transition-colors duration-150 hover:bg-rose-50 hover:text-rose-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-500 dark:text-slate-400 dark:hover:bg-rose-950/60 dark:hover:text-rose-400"
        >
          <Trash2 className="h-4 w-4" aria-hidden="true" />
        </button>
        {isSelected && (
          <button
            type="button"
            onClick={onClearSelection}
            aria-label="Cancelar seleção"
            className="flex h-11 w-11 items-center justify-center rounded-xl text-slate-400 transition-colors duration-150 hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--accent)] dark:text-slate-500 dark:hover:bg-slate-800 dark:hover:text-slate-200"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </button>
        )}
      </div>
    </article>
  );
}
