import { describe, expect, it } from 'vitest';
import { createTaskSchema, updateTaskSchema } from '@/lib/validation';

describe('task validation', () => {
  it('trims the title and turns the date string into a Date', () => {
    const parsed = createTaskSchema.parse({ title: '  Learn server actions ', dueDate: '2026-10-01' });
    expect(parsed.title).toBe('Learn server actions');
    expect(parsed.dueDate).toBeInstanceOf(Date);
  });

  it('rejects an empty title', () => {
    expect(createTaskSchema.safeParse({ title: '   ' }).success).toBe(false);
  });

  it('needs at least one field for an update', () => {
    expect(updateTaskSchema.safeParse({}).success).toBe(false);
    expect(updateTaskSchema.safeParse({ status: 'DONE' }).success).toBe(true);
    expect(updateTaskSchema.safeParse({ status: 'LATER' }).success).toBe(false);
  });
});
