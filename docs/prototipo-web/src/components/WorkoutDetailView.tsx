import { ArrowLeft, Dumbbell, Pencil, Trash2 } from 'lucide-react';
import { formatRelativeDay, type Workout } from '../utils/workoutStore';

interface WorkoutDetailViewProps {
  workout: Workout;
  onBack: () => void;
  onEdit: (workout: Workout) => void;
  onDelete: (workout: Workout) => void;
}

export function WorkoutDetailView({
  workout,
  onBack,
  onEdit,
  onDelete,
}: WorkoutDetailViewProps) {
  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-6 sm:px-8 sm:py-8">
      <div className="flex items-center justify-between gap-4">
        <button
          type="button"
          onClick={onBack}
          className="flex min-h-[44px] items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-200/70 hover:text-slate-900 whitespace-nowrap shrink-0 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          <span>Voltar</span>
        </button>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onEdit(workout)}
            className="flex min-h-[44px] items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 whitespace-nowrap shrink-0 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
          >
            <Pencil className="h-4 w-4" aria-hidden="true" />
            <span>Editar</span>
          </button>
          <button
            type="button"
            onClick={() => onDelete(workout)}
            className="flex min-h-[44px] items-center gap-2 rounded-xl border border-rose-200 bg-white px-4 py-2 text-sm font-medium text-rose-600 transition-colors hover:bg-rose-50 whitespace-nowrap shrink-0 dark:border-rose-900/60 dark:bg-slate-900 dark:text-rose-400 dark:hover:bg-rose-950/50"
          >
            <Trash2 className="h-4 w-4" aria-hidden="true" />
            <span>Excluir</span>
          </button>
        </div>
      </div>

      <section className="mt-8 rounded-2xl border border-slate-200 bg-white p-8 text-center dark:border-slate-800 dark:bg-slate-900">
        <span
          style={{
            backgroundColor: 'var(--accent-soft)',
            color: 'var(--accent)',
          }}
          className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl"
        >
          <Dumbbell className="h-7 w-7" aria-hidden="true" />
        </span>

        <h2 className="mt-5 text-2xl font-bold text-slate-900 dark:text-slate-100">
          {workout.name}
        </h2>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          {workout.focus || 'Sem foco muscular definido'} · {formatRelativeDay(workout.updatedAt)}
        </p>

        <p className="mx-auto mt-6 max-w-md text-sm leading-relaxed text-slate-600 dark:text-slate-300">
          A lista de exercícios, séries e cargas deste treino será construída na próxima etapa do aplicativo.
        </p>
      </section>
    </div>
  );
}
