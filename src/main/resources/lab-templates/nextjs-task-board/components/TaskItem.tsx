'use client';

import Link from 'next/link';
import { useTransition } from 'react';
import { deleteTaskAction, setStatusAction } from '@/app/actions';
import type { Task } from '@/generated/prisma/client';

const NEXT_STATUS = { TODO: 'IN_PROGRESS', IN_PROGRESS: 'DONE', DONE: 'TODO' } as const;

export function TaskItem({ task }: { task: Task }) {
  // useTransition keeps the UI responsive while the server action runs.
  const [pending, startTransition] = useTransition();

  return (
    <li className="card row" style={{ justifyContent: 'space-between', opacity: pending ? 0.6 : 1 }}>
      <Link href={`/tasks/${task.id}`}>{task.title}</Link>
      <span className="row">
        <button type="button" onClick={() => startTransition(() => setStatusAction(task.id, NEXT_STATUS[task.status]))}>
          {task.status.replace('_', ' ')}
        </button>
        <button type="button" onClick={() => startTransition(() => deleteTaskAction(task.id))} aria-label={`Delete ${task.title}`}>
          x
        </button>
      </span>
    </li>
  );
}
