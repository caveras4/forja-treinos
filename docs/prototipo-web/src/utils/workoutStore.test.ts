import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createWorkout,
  deleteWorkout,
  getInitialWorkouts,
  hasExceededPointerThreshold,
  toggleSelectedWorkoutId,
  updateWorkout,
  validateWorkoutInput,
} from './workoutStore';
import { resolveIsDark } from './themePalettes';

test('getInitialWorkouts seeds Treino A, Treino B, and Treino C', () => {
  const initial = getInitialWorkouts(1000);
  assert.equal(initial.length, 3);
  assert.deepEqual(
    initial.map((w) => w.name),
    ['Treino A', 'Treino B', 'Treino C'],
  );
});

test('validateWorkoutInput rejects blank and overly short names', () => {
  assert.equal(validateWorkoutInput('   ', 'Peito').valid, false);
  assert.equal(validateWorkoutInput('A', 'Peito').valid, false);
  assert.equal(validateWorkoutInput('Treino D', 'Core').valid, true);
});

test('createWorkout appends a trimmed workout with sequential orderIndex', () => {
  const initial = getInitialWorkouts(1000);
  const updated = createWorkout(
    initial,
    { name: '  Treino D  ', focus: '  Cardio e Core  ', colorKey: 'crimson' },
    () => 'treino-d',
    2000,
  );
  assert.equal(updated.length, 4);
  assert.equal(updated[3].id, 'treino-d');
  assert.equal(updated[3].name, 'Treino D');
  assert.equal(updated[3].focus, 'Cardio e Core');
  assert.equal(updated[3].orderIndex, 3);
  assert.equal(updated[3].colorKey, 'crimson');
});

test('updateWorkout updates matching workout without mutating others', () => {
  const initial = getInitialWorkouts(1000);
  const updated = updateWorkout(
    initial,
    'treino-b',
    { name: 'Treino B - Dorsal', focus: 'Costas completas', colorKey: 'cobalt' },
    3000,
  );
  assert.equal(updated[1].name, 'Treino B - Dorsal');
  assert.equal(updated[1].focus, 'Costas completas');
  assert.equal(updated[1].updatedAt, 3000);
  assert.equal(updated[0].name, 'Treino A');
});

test('deleteWorkout removes target and re-indexes remaining items', () => {
  const initial = getInitialWorkouts(1000);
  const afterDelete = deleteWorkout(initial, 'treino-b');
  assert.equal(afterDelete.length, 2);
  assert.deepEqual(
    afterDelete.map((w) => ({ id: w.id, orderIndex: w.orderIndex })),
    [
      { id: 'treino-a', orderIndex: 0 },
      { id: 'treino-c', orderIndex: 1 },
    ],
  );
});

test('resolveIsDark honors explicit theme modes and falls back to system preference', () => {
  assert.equal(resolveIsDark('dark', false), true);
  assert.equal(resolveIsDark('light', true), false);
  assert.equal(resolveIsDark('system', true), true);
  assert.equal(resolveIsDark('system', false), false);
});

test('hasExceededPointerThreshold detects scroll movement vs stationary hold', () => {
  assert.equal(hasExceededPointerThreshold({ x: 100, y: 100 }, { x: 104, y: 103 }, 10), false);
  assert.equal(hasExceededPointerThreshold({ x: 100, y: 100 }, { x: 100, y: 115 }, 10), true);
});

test('toggleSelectedWorkoutId selects and deselects cleanly', () => {
  assert.equal(toggleSelectedWorkoutId(null, 'treino-a'), 'treino-a');
  assert.equal(toggleSelectedWorkoutId('treino-a', 'treino-a'), null);
  assert.equal(toggleSelectedWorkoutId('treino-a', 'treino-b'), 'treino-b');
});
