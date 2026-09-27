import type { Task } from '@/generated/prisma/client';
import { TaskItem } from '@/components/TaskItem';

// A server component (the default): it renders on the server and sends only HTML to the browser.
export function TaskList({ tasks }: { tasks: Task[] }) {
  if (tasks.length === 0) {
    return <p className="muted">No tasks yet - add the first one above.</p>;
  }
  return (
    <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: 8 }}>
      {tasks.map((task) => (
        <TaskItem key={task.id} task={task} />
      ))}
    </ul>
  );
}
