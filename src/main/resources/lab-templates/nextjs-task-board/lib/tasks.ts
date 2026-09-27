import { prisma } from '@/lib/db';
import type { CreateTaskInput, TaskStatusValue, UpdateTaskInput } from '@/lib/validation';

// Data access: the only file that talks to the database. Pages, API routes and
// server actions all call these functions instead of using Prisma directly.

export function listTasks(status?: TaskStatusValue) {
  return prisma.task.findMany({
    where: status ? { status } : undefined,
    orderBy: [{ status: 'asc' }, { createdAt: 'desc' }],
  });
}

export function getTask(id: string) {
  return prisma.task.findUnique({ where: { id } });
}

export function createTask(input: CreateTaskInput) {
  return prisma.task.create({ data: input });
}

export async function updateTask(id: string, input: UpdateTaskInput) {
  // updateMany does not throw when the id is unknown; count tells us whether a row matched.
  const { count } = await prisma.task.updateMany({ where: { id }, data: input });
  return count === 0 ? null : getTask(id);
}

export async function deleteTask(id: string) {
  const { count } = await prisma.task.deleteMany({ where: { id } });
  return count > 0;
}
