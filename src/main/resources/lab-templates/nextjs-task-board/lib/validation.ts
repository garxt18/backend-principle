import { z } from 'zod';

// One schema validates input everywhere: API routes, server actions and forms.
export const taskStatus = z.enum(['TODO', 'IN_PROGRESS', 'DONE']);

export const createTaskSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(120),
  description: z.string().trim().max(2000).optional(),
  dueDate: z.coerce.date().optional(),
});

// PATCH: every field optional, but at least one must be sent.
export const updateTaskSchema = createTaskSchema
  .extend({ status: taskStatus })
  .partial()
  .refine((body) => Object.keys(body).length > 0, 'Send at least one field to update');

export type CreateTaskInput = z.infer<typeof createTaskSchema>;
export type UpdateTaskInput = z.infer<typeof updateTaskSchema>;
export type TaskStatusValue = z.infer<typeof taskStatus>;
