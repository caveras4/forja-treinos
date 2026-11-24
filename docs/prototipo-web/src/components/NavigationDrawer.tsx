import { useEffect } from 'react';
import {
  Check,
  Dumbbell,
  Monitor,
  Moon,
  Sun,
  X,
} from 'lucide-react';
import {
  ACCENT_PALETTES,
  type AccentPaletteId,
  type ThemeMode,
} from '../utils/themePalettes';

interface NavigationDrawerProps {
  isOpen: boolean;
  selectedPalette: AccentPaletteId;
  themeMode: ThemeMode;
  workoutCount: number;
  onClose: () => void;
  onSelectPalette: (palette: AccentPaletteId) => void;
  onSelectThemeMode: (mode: ThemeMode) => void;
  onNavigateHome: () => void;
}

export function NavigationDrawer({
  isOpen,
  selectedPalette,
  themeMode,
  workoutCount,
  onClose,
  onSelectPalette,
  onSelectThemeMode,
  onNavigateHome,
}: NavigationDrawerProps) {
  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [isOpen, onClose]);

  const drawerBody = (
    <div className="flex h-full flex-col justify-between">
      <div>
        <div className="flex h-14 items-center justify-between gap-3 border-b border-slate-200/80 px-5 dark:border-slate-800/80">
          <div className="flex items-center gap-2.5">
            <span
              style={{
                backgroundColor: 'var(--accent-soft)',
                color: 'var(--accent)',
              }}
              className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg"
            >
              <Dumbbell className="h-4 w-4" aria-hidden="true" />
            </span>
            <span className="text-base font-bold tracking-tight text-slate-900 dark:text-slate-100 whitespace-nowrap">
              Forja Treinos
            </span>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar menu"
            className="flex h-11 w-11 items-center justify-center rounded-xl text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 lg:hidden dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <div className="p-5">
          <nav aria-label="Menu principal" className="space-y-1">
          <button
            type="button"
            onClick={() => {
              onNavigateHome();
              onClose();
            }}
            style={{
              backgroundColor: 'var(--accent-soft)',
              color: 'var(--accent)',
            }}
            className="flex min-h-[44px] w-full items-center justify-between rounded-xl px-3.5 py-2.5 text-sm font-semibold transition-colors"
          >
            <span className="flex items-center gap-3 whitespace-nowrap">
              <Dumbbell className="h-4 w-4 shrink-0" aria-hidden="true" />
              <span>Meus Treinos</span>
            </span>
            <span className="tabular-nums text-xs font-semibold">
              {workoutCount}
            </span>
          </button>
        </nav>

        <div className="my-6 h-px bg-slate-200 dark:bg-slate-800" />

        <div>
          <h3 className="px-1 text-xs font-semibold text-slate-500 dark:text-slate-400">
            Modo de exibição
          </h3>
          <div
            role="group"
            aria-label="Selecionar modo de exibição"
            className="mt-2.5 grid grid-cols-3 gap-1.5 rounded-xl bg-slate-100 p-1 dark:bg-slate-800/80"
          >
            {(
              [
                { id: 'system', label: 'Sistema', Icon: Monitor },
                { id: 'light', label: 'Claro', Icon: Sun },
                { id: 'dark', label: 'Escuro', Icon: Moon },
              ] as const
            ).map(({ id, label, Icon }) => {
              const active = themeMode === id;
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => onSelectThemeMode(id)}
                  aria-pressed={active}
                  className={`flex min-h-[44px] flex-col items-center justify-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium transition-colors whitespace-nowrap ${
                    active
                      ? 'bg-white text-slate-900 shadow-xs dark:bg-slate-950 dark:text-slate-100'
                      : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200'
                  }`}
                >
                  <Icon className="h-4 w-4" aria-hidden="true" />
                  <span>{label}</span>
                </button>
              );
            })}
          </div>
        </div>

        <div className="mt-6">
          <h3 className="px-1 text-xs font-semibold text-slate-500 dark:text-slate-400">
            Paleta de cores
          </h3>
          <div className="mt-2.5 grid grid-cols-2 gap-2">
            {Object.values(ACCENT_PALETTES).map((palette) => {
              const selected = palette.id === selectedPalette;
              return (
                <button
                  key={palette.id}
                  type="button"
                  onClick={() => onSelectPalette(palette.id)}
                  aria-pressed={selected}
                  className={`flex min-h-[44px] items-center gap-2.5 rounded-xl border px-3 py-2 text-left text-xs font-medium transition-colors whitespace-nowrap ${
                    selected
                      ? 'border-slate-900 bg-slate-50 text-slate-900 dark:border-slate-200 dark:bg-slate-800 dark:text-slate-100'
                      : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300 dark:hover:border-slate-700'
                  }`}
                >
                  <span
                    aria-hidden="true"
                    style={{ backgroundColor: palette.swatchHex }}
                    className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-white"
                  >
                    {selected && <Check className="h-3 w-3" />}
                  </span>
                  <span className="truncate">{palette.name}</span>
                </button>
              );
            })}
            </div>
          </div>
        </div>
      </div>

      <p className="border-t border-slate-200 px-5 pb-5 pt-4 text-xs leading-relaxed text-slate-500 dark:border-slate-800 dark:text-slate-400">
        Outras opções do menu e a tela detalhada de exercícios serão adicionadas nas próximas etapas.
      </p>
    </div>
  );

  return (
    <>
      {/* Persistent side drawer on desktop/large tablet */}
      <aside className="hidden w-72 shrink-0 border-r border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-900 lg:block">
        {drawerBody}
      </aside>

      {/* Modal slide-over drawer on smartphone and compact tablet */}
      {isOpen && (
        <div
          className="fixed inset-0 z-50 flex lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="Menu de navegação"
        >
          <button
            type="button"
            aria-label="Fechar menu ao clicar fora"
            onClick={onClose}
            className="fixed inset-0 bg-slate-950/50 backdrop-blur-xs"
          />
          <aside className="relative z-10 h-full w-80 max-w-[85vw] border-r border-slate-200 bg-white shadow-2xl dark:border-slate-800 dark:bg-slate-900">
            {drawerBody}
          </aside>
        </div>
      )}
    </>
  );
}
