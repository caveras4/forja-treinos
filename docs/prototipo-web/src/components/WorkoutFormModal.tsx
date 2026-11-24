import { useEffect, useState, type FormEvent } from 'react';
import { Check, X } from 'lucide-react';
import { ACCENT_PALETTES, type AccentPaletteId } from '../utils/themePalettes';
import { validateWorkoutInput, type Workout } from '../utils/workoutStore';

interface WorkoutFormModalProps {
  isOpen: boolean;
  editingWorkout: Workout | null;
  defaultColorKey: AccentPaletteId;
  onClose: () => void;
  onSave: (data: { name: string; focus: string; colorKey: AccentPaletteId }) => void;
}

export function WorkoutFormModal({
  isOpen,
  editingWorkout,
  defaultColorKey,
  onClose,
  onSave,
}: WorkoutFormModalProps) {
  const [name, setName] = useState('');
  const [focus, setFocus] = useState('');
  const [colorKey, setColorKey] = useState<AccentPaletteId>(defaultColorKey);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;
    setName(editingWorkout?.name ?? '');
    setFocus(editingWorkout?.focus ?? '');
    setColorKey(editingWorkout?.colorKey ?? defaultColorKey);
    setError(null);
  }, [isOpen, editingWorkout, defaultColorKey]);

  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const result = validateWorkoutInput(name, focus);
    if (!result.valid) {
      setError(result.error ?? 'Verifique os campos informados.');
      return;
    }
    onSave({ name, focus, colorKey });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/50 p-0 backdrop-blur-xs sm:items-center sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="workout-form-title"
    >
      <div
        className="w-full max-w-md rounded-t-3xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-800 dark:bg-slate-900 sm:rounded-2xl"
      >
        <div className="mx-auto mb-4 h-1.5 w-10 rounded-full bg-slate-300 dark:bg-slate-700 sm:hidden" />

        <div className="flex items-center justify-between gap-4">
          <h2
            id="workout-form-title"
            className="text-lg font-bold text-slate-900 dark:text-slate-100"
          >
            {editingWorkout ? 'Editar treino' : 'Novo treino'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar janela"
            className="flex h-11 w-11 items-center justify-center rounded-xl text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <form onSubmit={handleSubmit} noValidate className="mt-5 space-y-4">
          <div>
            <label
              htmlFor="workout-name-input"
              className="block text-sm font-medium text-slate-700 dark:text-slate-300"
            >
              Nome do treino
            </label>
            <input
              id="workout-name-input"
              type="text"
              autoFocus
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (error) setError(null);
              }}
              placeholder="Ex.: Treino D"
              maxLength={48}
              className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-[var(--accent)] focus:outline-none focus:ring-2 focus:ring-[var(--accent-ring)] dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100 dark:placeholder:text-slate-500"
            />
            {error && (
              <p role="alert" className="mt-1.5 text-xs font-medium text-rose-600 dark:text-rose-400">
                {error}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="workout-focus-input"
              className="block text-sm font-medium text-slate-700 dark:text-slate-300"
            >
              Foco muscular (opcional)
            </label>
            <input
              id="workout-focus-input"
              type="text"
              value={focus}
              onChange={(e) => setFocus(e.target.value)}
              placeholder="Ex.: Peito, Ombros e Tríceps"
              maxLength={80}
              className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-[var(--accent)] focus:outline-none focus:ring-2 focus:ring-[var(--accent-ring)] dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100 dark:placeholder:text-slate-500"
            />
          </div>

          <div>
            <span className="block text-sm font-medium text-slate-700 dark:text-slate-300">
              Cor de identificação
            </span>
            <div className="mt-2 flex items-center gap-3">
              {Object.values(ACCENT_PALETTES).map((palette) => {
                const selected = palette.id === colorKey;
                return (
                  <button
                    key={palette.id}
                    type="button"
                    onClick={() => setColorKey(palette.id)}
                    aria-label={`Selecionar cor ${palette.name}`}
                    aria-pressed={selected}
                    style={{ backgroundColor: palette.swatchHex }}
                    className={`flex h-11 w-11 items-center justify-center rounded-full text-white transition-transform duration-150 hover:scale-105 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900 dark:focus-visible:outline-white ${
                      selected ? 'ring-2 ring-slate-900 ring-offset-2 dark:ring-white dark:ring-offset-slate-900' : ''
                    }`}
                  >
                    {selected && <Check className="h-5 w-5" aria-hidden="true" />}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={onClose}
              className="min-h-[44px] rounded-xl px-4 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900 whitespace-nowrap shrink-0 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
            >
              Cancelar
            </button>
            <button
              type="submit"
              style={{
                backgroundColor: 'var(--accent)',
                color: 'var(--accent-fg)',
              }}
              className="min-h-[44px] rounded-xl px-5 py-2 text-sm font-semibold shadow-sm transition-opacity hover:opacity-90 whitespace-nowrap shrink-0"
            >
              {editingWorkout ? 'Salvar alterações' : 'Salvar treino'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
