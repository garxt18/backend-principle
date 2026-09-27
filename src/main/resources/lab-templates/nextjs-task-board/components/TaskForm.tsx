'use client';

import { useActionState } from 'react';
import { createTaskAction, type FormState } from '@/app/actions';

// A client component: it needs state in the browser (pending/error), so it starts with 'use client'.
export function TaskForm() {
  const [state, formAction, pending] = useActionState<FormState, FormData>(createTaskAction, {});

  return (
    <form action={formAction} className="card" style={{ display: 'grid', gap: 10 }}>
      <input name="title" placeholder="What needs doing?" maxLength={120} required />
      <textarea name="description" placeholder="Details (optional)" rows={2} />
      <div className="row">
        <input name="dueDate" type="date" />
        <button type="submit" disabled={pending}>{pending ? 'Adding...' : 'Add task'}</button>
      </div>
      {state.error && <p className="error">{state.error}</p>}
    </form>
  );
}
