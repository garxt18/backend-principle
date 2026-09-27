# Task Board - Next.js 16 full-stack template

A small but complete Next.js app to rebuild line by line in Backend Playground's Rebuild Lab:

- **Database**: PostgreSQL with Prisma 7 (`prisma/schema.prisma`, migrations, `lib/db.ts`)
- **Validation**: one zod schema for API routes, server actions and forms (`lib/validation.ts`)
- **Data access**: `lib/tasks.ts` is the only file that queries the database
- **Server actions**: `app/actions.ts` - forms call server code directly
- **REST API**: route handlers in `app/api/tasks/**/route.ts`, protected by `proxy.ts` (x-api-key)
- **UI**: server components (`app/page.tsx`, `components/TaskList.tsx`) and client components
  (`components/TaskForm.tsx`, `components/TaskItem.tsx`)

## Run it

```bash
cp .env.example .env               # set DATABASE_URL and API_KEY
npm install
npx prisma migrate deploy          # creates the tasks table
npm run dev                        # http://localhost:3000
npm test                           # validation tests
```

Try the API:

```bash
curl localhost:3000/api/tasks
curl -X POST localhost:3000/api/tasks -H "x-api-key: $API_KEY" -H "content-type: application/json" \
     -d '{"title":"Rebuild this app"}'
```
