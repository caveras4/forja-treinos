/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useMemo, useState } from 'react';
import { Dumbbell, Menu, Moon, Plus, Sun } from 'lucide-react';
import { DeleteConfirmModal } from './components/DeleteConfirmModal';
import { NavigationDrawer } from './components/NavigationDrawer';
import { WorkoutCard } from './components/WorkoutCard';
import { WorkoutDetailView } from './components/WorkoutDetailView';
import { WorkoutFormModal } from './components/WorkoutFormModal';
import {
  ACCENT_PALETTES,
  isAccentPaletteId,
  isThemeMode,
  resolveIsDark,
  type AccentPaletteId,
  type ThemeMode,
} from './utils/themePalettes';
import {
  createWorkout,
  deleteWorkout,
  getInitialWorkouts,
  updateWorkout,
  type Workout,
} from './utils/workoutStore';

const STORAGE_WORKOUTS_KEY = 'forja_treinos_workouts_v1';
const STORAGE_PALETTE_KEY = 'forja_treinos_palette_v1';
const STORAGE_THEME_KEY = 'forja_treinos_theme_v1';

function loadStoredWorkouts(): Workout[] {
  try {
    const raw = window.localStorage.getItem(STORAGE_WORKOUTS_KEY);
    if (!raw) return getInitialWorkouts();
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed;
  } catch {
    // Fallback to initial seeded workouts if localStorage is unavailable or corrupted
  }
  return getInitialWorkouts();
}

function loadStoredPalette(): AccentPaletteId {
  try {
    const raw = window.localStorage.getItem(STORAGE_PALETTE_KEY);
    if (isAccentPaletteId(raw)) return raw;
  } catch {
    // Ignore storage access errors
  }
  return 'emerald';
}

function loadStoredThemeMode(): ThemeMode {
  try {
    const raw = window.localStorage.getItem(STORAGE_THEME_KEY);
    if (isThemeMode(raw)) return raw;
  } catch {
    // Ignore storage access errors
  }
  return 'system';
}

export default function App() {
  const [workouts, setWorkouts] = useState<Workout[]>(() => loadStoredWorkouts());
  const [selectedPalette, setSelectedPalette] = useState<AccentPaletteId>(() => loadStoredPalette());
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => loadStoredThemeMode());
  const [systemPrefersDark, setSystemPrefersDark] = useState<boolean>(() =>
    typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches,
  );

  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingWorkout, setEditingWorkout] = useState<Workout | null>(null);
  const [deleteCandidate, setDeleteCandidate] = useState<Workout | null>(null);
  const [selectedWorkoutId, setSelectedWorkoutId] = useState<string | null>(null);
  const [actionSelectedId, setActionSelectedId] = useState<string | null>(null);

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const listener = (event: MediaQueryListEvent) => setSystemPrefersDark(event.matches);
    media.addEventListener('change', listener);
    return () => media.removeEventListener('change', listener);
  }, []);

  const isDark = useMemo(
    () => resolveIsDark(themeMode, systemPrefersDark),
    [themeMode, systemPrefersDark],
  );

  useEffect(() => {
    const root = document.documentElement;
    root.classList.toggle('dark', isDark);
    const palette = ACCENT_PALETTES[selectedPalette];
    const tokens = isDark ? palette.dark : palette.light;
    root.style.setProperty('--accent', tokens.accent);
    root.style.setProperty('--accent-hover', tokens.accentHover);
    root.style.setProperty('--accent-fg', tokens.accentFg);
    root.style.setProperty('--accent-soft', tokens.accentSoft);
    root.style.setProperty('--accent-ring', tokens.accentRing);
  }, [isDark, selectedPalette]);

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_WORKOUTS_KEY, JSON.stringify(workouts));
    } catch {
      // Ignore storage quota errors
    }
  }, [workouts]);

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_PALETTE_KEY, selectedPalette);
    } catch {
      // Ignore storage quota errors
    }
  }, [selectedPalette]);

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_THEME_KEY, themeMode);
    } catch {
      // Ignore storage quota errors
    }
  }, [themeMode]);

  useEffect(() => {
    if (!actionSelectedId) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setActionSelectedId(null);
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [actionSelectedId]);

  const activeDetailWorkout = useMemo(
    () => workouts.find((w) => w.id === selectedWorkoutId) ?? null,
    [workouts, selectedWorkoutId],
  );

  const selectedActionWorkout = useMemo(
    () => workouts.find((w) => w.id === actionSelectedId) ?? null,
    [workouts, actionSelectedId],
  );

  const handleOpenCreate = () => {
    setActionSelectedId(null);
    setEditingWorkout(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (workout: Workout) => {
    setActionSelectedId(null);
    setEditingWorkout(workout);
    setIsFormOpen(true);
  };

  const handleRequestDelete = (workout: Workout) => {
    setActionSelectedId(null);
    setDeleteCandidate(workout);
  };

  const handleSaveWorkout = (data: {
    name: string;
    focus: string;
    colorKey: AccentPaletteId;
  }) => {
    if (editingWorkout) {
      setWorkouts((prev) => updateWorkout(prev, editingWorkout.id, data));
    } else {
      setWorkouts((prev) => createWorkout(prev, data));
    }
    setIsFormOpen(false);
    setEditingWorkout(null);
  };

  const handleConfirmDelete = () => {
    if (!deleteCandidate) return;
    const deletedId = deleteCandidate.id;
    setWorkouts((prev) => deleteWorkout(prev, deletedId));
    setDeleteCandidate(null);
    setActionSelectedId((prev) => (prev === deletedId ? null : prev));
    if (selectedWorkoutId === deletedId) {
      setSelectedWorkoutId(null);
    }
  };

  const handleQuickToggleDark = () => {
    setThemeMode(isDark ? 'light' : 'dark');
  };

  return (
    <div className="flex min-h-screen bg-slate-50 text-slate-900 dark:bg-[#090d16] dark:text-slate-100">
      <NavigationDrawer
        isOpen={isDrawerOpen}
        selectedPalette={selectedPalette}
        themeMode={themeMode}
        workoutCount={workouts.length}
        onClose={() => setIsDrawerOpen(false)}
        onSelectPalette={setSelectedPalette}
        onSelectThemeMode={setThemeMode}
        onNavigateHome={() => setSelectedWorkoutId(null)}
      />

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-14 items-center justify-between gap-4 border-b border-slate-200/80 bg-slate-50/90 px-4 backdrop-blur-md dark:border-slate-800/80 dark:bg-[#090d16]/90 sm:px-6">
          <div className="flex min-w-0 items-center gap-3">
            <button
              type="button"
              onClick={() => setIsDrawerOpen(true)}
              aria-label="Abrir menu"
              className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-slate-700 transition-colors hover:bg-slate-200/70 hover:text-slate-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--accent)] lg:hidden dark:text-slate-200 dark:hover:bg-slate-800 dark:hover:text-white"
            >
              <Menu className="h-6 w-6" aria-hidden="true" />
            </button>
            <h1 className="truncate text-lg font-bold tracking-tight text-slate-900 dark:text-slate-100 sm:text-xl">
              {activeDetailWorkout ? activeDetailWorkout.name : 'Meus Treinos'}
            </h1>
          </div>

          <button
            type="button"
            onClick={handleQuickToggleDark}
            aria-label={isDark ? 'Mudar para modo claro' : 'Mudar para modo escuro'}
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-slate-600 transition-colors hover:bg-slate-200/70 hover:text-slate-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--accent)] dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
          >
            {isDark ? (
              <Sun className="h-5 w-5" aria-hidden="true" />
            ) : (
              <Moon className="h-5 w-5" aria-hidden="true" />
            )}
          </button>
        </header>

        <main
          onClick={() => {
            if (actionSelectedId) setActionSelectedId(null);
          }}
          className="flex-1 pb-28"
        >
          {activeDetailWorkout ? (
            <WorkoutDetailView
              workout={activeDetailWorkout}
              onBack={() => setSelectedWorkoutId(null)}
              onEdit={handleOpenEdit}
              onDelete={handleRequestDelete}
            />
          ) : (
            <div className="mx-auto w-full max-w-6xl px-4 py-5 sm:px-8 sm:py-7">
              <div className="mb-5 flex items-baseline justify-between gap-4">
                {selectedActionWorkout ? (
                  <p className="text-sm font-medium text-slate-700 dark:text-slate-200">
                    <span className="font-semibold">{selectedActionWorkout.name}</span> selecionado · escolha editar ou excluir
                  </p>
                ) : (
                  <p className="text-sm text-slate-500 dark:text-slate-400">
                    <span className="tabular-nums font-semibold text-slate-700 dark:text-slate-200">
                      {workouts.length}
                    </span>{' '}
                    {workouts.length === 1 ? 'treino salvo no dispositivo' : 'treinos salvos no dispositivo'}
                  </p>
                )}
              </div>

              {workouts.length === 0 ? (
                <section className="mx-auto mt-10 max-w-md rounded-2xl border border-slate-200 bg-white p-8 text-center dark:border-slate-800 dark:bg-slate-900">
                  <span
                    style={{
                      backgroundColor: 'var(--accent-soft)',
                      color: 'var(--accent)',
                    }}
                    className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl"
                  >
                    <Dumbbell className="h-7 w-7" aria-hidden="true" />
                  </span>
                  <h2 className="mt-4 text-lg font-bold text-slate-900 dark:text-slate-100">
                    Nenhum treino cadastrado
                  </h2>
                  <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
                    Use o botão + no canto inferior direito ou crie seu primeiro treino agora.
                  </p>
                  <button
                    type="button"
                    onClick={handleOpenCreate}
                    style={{
                      backgroundColor: 'var(--accent)',
                      color: 'var(--accent-fg)',
                    }}
                    className="mt-6 inline-flex min-h-[44px] items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-semibold shadow-sm transition-opacity hover:opacity-90 whitespace-nowrap"
                  >
                    <Plus className="h-4 w-4" aria-hidden="true" />
                    <span>Criar primeiro treino</span>
                  </button>
                </section>
              ) : (
                <div
                  data-testid="workouts-list"
                  className="grid grid-cols-1 gap-3.5 md:grid-cols-2 sm:gap-4"
                >
                  {workouts.map((workout) => (
                    <WorkoutCard
                      key={workout.id}
                      workout={workout}
                      isDark={isDark}
                      isSelected={actionSelectedId === workout.id}
                      onSelectForActions={(item) => setActionSelectedId(item.id)}
                      onClearSelection={() => setActionSelectedId(null)}
                      onOpen={(item) => {
                        setActionSelectedId(null);
                        setSelectedWorkoutId(item.id);
                      }}
                      onEdit={handleOpenEdit}
                      onDelete={handleRequestDelete}
                    />
                  ))}
                </div>
              )}
            </div>
          )}
        </main>
      </div>

      {!activeDetailWorkout && (
        <button
          type="button"
          onClick={handleOpenCreate}
          aria-label="Criar novo treino"
          style={{
            backgroundColor: 'var(--accent)',
            color: 'var(--accent-fg)',
          }}
          className="fixed bottom-6 right-6 z-40 flex h-14 w-14 items-center justify-center rounded-full shadow-lg transition-transform duration-150 hover:scale-105 active:scale-95 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--accent)] sm:bottom-8 sm:right-8 sm:h-15 sm:w-15"
        >
          <Plus className="h-7 w-7" aria-hidden="true" />
        </button>
      )}

      <WorkoutFormModal
        isOpen={isFormOpen}
        editingWorkout={editingWorkout}
        defaultColorKey={selectedPalette}
        onClose={() => {
          setIsFormOpen(false);
          setEditingWorkout(null);
        }}
        onSave={handleSaveWorkout}
      />

      <DeleteConfirmModal
        workout={deleteCandidate}
        onCancel={() => setDeleteCandidate(null)}
        onConfirm={handleConfirmDelete}
      />
    </div>
  );
}

