import { NextResponse, type NextRequest } from 'next/server';
import { deleteTask, getTask, updateTask } from '@/lib/tasks';
import { updateTaskSchema } from '@/lib/validation';

// In Next.js 15+ route params arrive as a Promise - await them before use.
type Context = { params: Promise<{ id: string }> };

// GET /api/tasks/:id
export async function GET(_request: NextRequest, { params }: Context) {
  const { id } = await params;
  const task = await getTask(id);
  if (!task) {
    return NextResponse.json({ error: 'Task not found' }, { status: 404 });
  }
  return NextResponse.json(task);
}

// PATCH /api/tasks/:id   body: any of { title, description, dueDate, status }
export async function PATCH(request: NextRequest, { params }: Context) {
  const { id } = await params;
  const parsed = updateTaskSchema.safeParse(await request.json().catch(() => null));
  if (!parsed.success) {
    return NextResponse.json({ error: 'Validation failed', issues: parsed.error.issues }, { status: 400 });
  }
  const task = await updateTask(id, parsed.data);
  if (!task) {
    return NextResponse.json({ error: 'Task not found' }, { status: 404 });
  }
  return NextResponse.json(task);
}

// DELETE /api/tasks/:id
export async function DELETE(_request: NextRequest, { params }: Context) {
  const { id } = await params;
  const deleted = await deleteTask(id);
  return deleted ? new NextResponse(null, { status: 204 }) : NextResponse.json({ error: 'Task not found' }, { status: 404 });
}
