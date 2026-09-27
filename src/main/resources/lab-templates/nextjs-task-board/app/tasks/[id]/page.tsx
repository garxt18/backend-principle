import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { getTask } from '@/lib/tasks';

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const task = await getTask((await params).id);
  return { title: task ? `${task.title} - Task Board` : 'Task not found' };
}

// /tasks/[id] - the folder name in brackets becomes a route parameter.
export default async function TaskPage({ params }: Props) {
  const { id } = await params;
  const task = await getTask(id);
  if (!task) {
    notFound(); // renders the 404 page and stops here
  }
  return (
    <article className="card">
      <Link href="/">Back to all tasks</Link>
      <h1>{task.title}</h1>
      <p className="muted">
        {task.status.replace('_', ' ')}
        {task.dueDate && ` - due ${task.dueDate.toISOString().slice(0, 10)}`}
      </p>
      {task.description && <p>{task.description}</p>}
    </article>
  );
}
