import type { AccentPaletteId } from './themePalettes';

export interface Workout {
  id: string;
  name: string;
  focus: string;
  colorKey: AccentPaletteId;
  orderIndex: number;
  updatedAt: number;
}

export interface WorkoutInput {
  name: string;
  focus: string;
  colorKey?: AccentPaletteId;
}

export interface ValidationResult {
  valid: boolean;
  error?: string;
}

export function getInitialWorkouts(now = Date.now()): Workout[] {
  return [
    {
      id: 'treino-a',
      name: 'Treino A',
      focus: 'Peito, Ombros e Tríceps',
      colorKey: 'emerald',
      orderIndex: 0,
      updatedAt: now,
    },
    {
      id: 'treino-b',
      name: 'Treino B',
      focus: 'Costas, Bíceps e Abdômen',
      colorKey: 'cobalt',
      orderIndex: 1,
      updatedAt: now,
    },
    {
      id: 'treino-c',
      name: 'Treino C',
      focus: 'Pernas e Glúteos',
      colorKey: 'amber',
      orderIndex: 2,
      updatedAt: now,
    },
  ];
}

export function validateWorkoutInput(name: string, focus: string): ValidationResult {
  const trimmedName = name.trim();
  if (!trimmedName) {
    return { valid: false, error: 'Informe o nome do treino.' };
  }
  if (trimmedName.length < 2) {
    return { valid: false, error: 'O nome deve ter pelo menos 2 caracteres.' };
  }
  if (trimmedName.length > 48) {
    return { valid: false, error: 'O nome deve ter no máximo 48 caracteres.' };
  }
  if (focus.trim().length > 80) {
    return { valid: false, error: 'O grupo muscular deve ter no máximo 80 caracteres.' };
  }
  return { valid: true };
}

export function createWorkout(
  list: Workout[],
  input: WorkoutInput,
  idGenerator: () => string = () => `treino-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
  now: number = Date.now(),
): Workout[] {
  const validation = validateWorkoutInput(input.name, input.focus);
  if (!validation.valid) {
    throw new Error(validation.error);
  }

  const nextOrder = list.length > 0 ? Math.max(...list.map((w) => w.orderIndex)) + 1 : 0;
  const newWorkout: Workout = {
    id: idGenerator(),
    name: input.name.trim(),
    focus: input.focus.trim(),
    colorKey: input.colorKey ?? 'emerald',
    orderIndex: nextOrder,
    updatedAt: now,
  };

  return [...list, newWorkout];
}

export function updateWorkout(
  list: Workout[],
  id: string,
  input: WorkoutInput,
  now: number = Date.now(),
): Workout[] {
  const validation = validateWorkoutInput(input.name, input.focus);
  if (!validation.valid) {
    throw new Error(validation.error);
  }

  return list.map((workout) => {
    if (workout.id !== id) return workout;
    return {
      ...workout,
      name: input.name.trim(),
      focus: input.focus.trim(),
      colorKey: input.colorKey ?? workout.colorKey,
      updatedAt: now,
    };
  });
}

export function deleteWorkout(list: Workout[], id: string): Workout[] {
  return list
    .filter((workout) => workout.id !== id)
    .map((workout, index) => ({
      ...workout,
      orderIndex: index,
    }));
}

export function formatRelativeDay(timestamp: number, now: number = Date.now()): string {
  const diffMs = Math.max(0, now - timestamp);
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  if (diffHours < 24) return 'Hoje';
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays === 1) return 'Ontem';
  return `Há ${diffDays} dias`;
}

export function hasExceededPointerThreshold(
  start: { x: number; y: number },
  current: { x: number; y: number },
  thresholdPx: number = 10,
): boolean {
  const dx = current.x - start.x;
  const dy = current.y - start.y;
  return Math.hypot(dx, dy) > thresholdPx;
}

export function toggleSelectedWorkoutId(
  currentSelectedId: string | null,
  targetId: string,
): string | null {
  return currentSelectedId === targetId ? null : targetId;
}

