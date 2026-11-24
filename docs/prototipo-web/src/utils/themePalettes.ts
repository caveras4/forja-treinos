export type AccentPaletteId = 'emerald' | 'cobalt' | 'amber' | 'crimson';

export type ThemeMode = 'system' | 'light' | 'dark';

export interface PaletteDefinition {
  id: AccentPaletteId;
  name: string;
  swatchHex: string;
  light: {
    accent: string;
    accentHover: string;
    accentFg: string;
    accentSoft: string;
    accentRing: string;
  };
  dark: {
    accent: string;
    accentHover: string;
    accentFg: string;
    accentSoft: string;
    accentRing: string;
  };
}

export const ACCENT_PALETTES: Record<AccentPaletteId, PaletteDefinition> = {
  emerald: {
    id: 'emerald',
    name: 'Esmeralda',
    swatchHex: '#059669',
    light: {
      accent: '#059669',
      accentHover: '#047857',
      accentFg: '#ffffff',
      accentSoft: 'rgba(5, 150, 105, 0.12)',
      accentRing: 'rgba(5, 150, 105, 0.35)',
    },
    dark: {
      accent: '#34d399',
      accentHover: '#10b981',
      accentFg: '#022c22',
      accentSoft: 'rgba(52, 211, 153, 0.15)',
      accentRing: 'rgba(52, 211, 153, 0.42)',
    },
  },
  cobalt: {
    id: 'cobalt',
    name: 'Cobalto',
    swatchHex: '#2563eb',
    light: {
      accent: '#2563eb',
      accentHover: '#1d4ed8',
      accentFg: '#ffffff',
      accentSoft: 'rgba(37, 99, 235, 0.12)',
      accentRing: 'rgba(37, 99, 235, 0.35)',
    },
    dark: {
      accent: '#60a5fa',
      accentHover: '#3b82f6',
      accentFg: '#0c1e3e',
      accentSoft: 'rgba(96, 165, 250, 0.15)',
      accentRing: 'rgba(96, 165, 250, 0.42)',
    },
  },
  amber: {
    id: 'amber',
    name: 'Âmbar',
    swatchHex: '#d97706',
    light: {
      accent: '#d97706',
      accentHover: '#b45309',
      accentFg: '#ffffff',
      accentSoft: 'rgba(217, 119, 6, 0.12)',
      accentRing: 'rgba(217, 119, 6, 0.35)',
    },
    dark: {
      accent: '#fbbf24',
      accentHover: '#f59e0b',
      accentFg: '#271702',
      accentSoft: 'rgba(251, 191, 36, 0.15)',
      accentRing: 'rgba(251, 191, 36, 0.42)',
    },
  },
  crimson: {
    id: 'crimson',
    name: 'Carmesim',
    swatchHex: '#e11d48',
    light: {
      accent: '#e11d48',
      accentHover: '#be123c',
      accentFg: '#ffffff',
      accentSoft: 'rgba(225, 29, 72, 0.12)',
      accentRing: 'rgba(225, 29, 72, 0.35)',
    },
    dark: {
      accent: '#fb7185',
      accentHover: '#f43f5e',
      accentFg: '#310612',
      accentSoft: 'rgba(251, 113, 133, 0.15)',
      accentRing: 'rgba(251, 113, 133, 0.42)',
    },
  },
};

export function resolveIsDark(mode: ThemeMode, systemPrefersDark: boolean): boolean {
  if (mode === 'dark') return true;
  if (mode === 'light') return false;
  return systemPrefersDark;
}

export function isAccentPaletteId(value: string | null): value is AccentPaletteId {
  return value === 'emerald' || value === 'cobalt' || value === 'amber' || value === 'crimson';
}

export function isThemeMode(value: string | null): value is ThemeMode {
  return value === 'system' || value === 'light' || value === 'dark';
}
