import { TaskForm } from '@/components/TaskForm';
import { TaskList } from '@/components/TaskList';
import { listTasks } from '@/lib/tasks';

// Always render on request: the list changes whenever a task is added.
export const dynamic = 'force-dynamic';

// A server component can be async and read the database directly - no API call, no useEffect.
export default async function HomePage() {
  const tasks = await listTasks();
  return (
    <>
      <h1>Task Board</h1>
      <p className="muted">{tasks.filter((t) => t.status === 'DONE').length} of {tasks.length} done</p>
      <TaskForm />
      <h2>Tasks</h2>
      <TaskList tasks={tasks} />
    </>
  );
}
