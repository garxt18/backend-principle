import { NextResponse, type NextRequest } from 'next/server';
import { createTask, listTasks } from '@/lib/tasks';
import { createTaskSchema, taskStatus } from '@/lib/validation';

// GET /api/tasks?status=TODO
export async function GET(request: NextRequest) {
  const status = request.nextUrl.searchParams.get('status');
  const parsed = status ? taskStatus.safeParse(status) : null;
  if (parsed && !parsed.success) {
    return NextResponse.json({ error: 'status must be TODO, IN_PROGRESS or DONE' }, { status: 400 });
  }
  const tasks = await listTasks(parsed?.data);
  return NextResponse.json(tasks);
}

// POST /api/tasks   body: { "title": "...", "description": "...", "dueDate": "2026-10-01" }
export async function POST(request: NextRequest) {
  const body = await request.json().catch(() => null);
  const parsed = createTaskSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: 'Validation failed', issues: parsed.error.issues }, { status: 400 });
  }
  const task = await createTask(parsed.data);
  return NextResponse.json(task, { status: 201, headers: { Location: `/api/tasks/${task.id}` } });
}
