'use server';

import { revalidatePath } from 'next/cache';
import { createTask, deleteTask, updateTask } from '@/lib/tasks';
import { createTaskSchema, taskStatus } from '@/lib/validation';

// Server actions: functions the browser can call directly from a <form>, without writing an API route.
// They run only on the server, so they can use the database and secrets safely.

export type FormState = { error?: string; ok?: boolean };

export async function createTaskAction(_prev: FormState, formData: FormData): Promise<FormState> {
  const parsed = createTaskSchema.safeParse({
    title: formData.get('title'),
    description: formData.get('description') || undefined,
    dueDate: formData.get('dueDate') || undefined,
  });
  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? 'Invalid input' };
  }
  await createTask(parsed.data);
  revalidatePath('/'); // the home page shows the list - rebuild it with the new task
  return { ok: true };
}

export async function setStatusAction(id: string, status: string) {
  await updateTask(id, { status: taskStatus.parse(status) });
  revalidatePath('/');
  revalidatePath(`/tasks/${id}`);
}

export async function deleteTaskAction(id: string) {
  await deleteTask(id);
  revalidatePath('/');
}
