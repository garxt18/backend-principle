package com.backendprinciple.playground.lab;

import static org.assertj.core.api.Assertions.assertThat;

import com.backendprinciple.playground.lab.BuildOrderPlanner.Candidate;
import com.backendprinciple.playground.lab.LineExplainer.Explanation;
import com.backendprinciple.playground.lab.FileLayer.Track;
import com.backendprinciple.playground.lab.LineExplainer.Note;
import com.backendprinciple.playground.lab.ZipProjectImporter.ImportedFile;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class NextjsSupportTest {

    private final LineExplainer explainer = new LineExplainer(JsonMapper.builder().build());

    private static FileLayer next(String path, String content) {
        return FileClassifier.layer(path, content, ProjectStack.NEXTJS);
    }

    private static Candidate file(String path, String content) {
        return new Candidate(path, next(path, content), content);
    }

    // ------------------------------------------------------------------ stack detection + layers

    @Test
    void detectsNextProjectsByConfigOrDependency() {
        assertThat(ProjectStack.detect(List.of(new ImportedFile("next.config.ts", "export default {};"))))
                .isEqualTo(ProjectStack.NEXTJS);
        assertThat(ProjectStack.detect(List.of(new ImportedFile("package.json", "{ \"dependencies\": { \"next\": \"16.0.0\" } }"))))
                .isEqualTo(ProjectStack.NEXTJS);
        assertThat(ProjectStack.detect(List.of(new ImportedFile("pom.xml", "<project/>"),
                new ImportedFile("frontend/package.json", "{ \"dependencies\": { \"react\": \"19.0.0\" } }"))))
                .isEqualTo(ProjectStack.SPRING);
        // A Spring project that carries a Next.js app deeper inside stays Spring - the root manifest decides.
        assertThat(ProjectStack.detect(List.of(new ImportedFile("pom.xml", "<project/>"),
                new ImportedFile("src/main/resources/lab-templates/next-app/next.config.ts", "export default {};"))))
                .isEqualTo(ProjectStack.SPRING);
        assertThat(ProjectStack.detect(List.of(new ImportedFile("next.config.ts", "export default {};"),
                new ImportedFile("tools/legacy/pom.xml", "<project/>"))))
                .isEqualTo(ProjectStack.NEXTJS);
    }

    @Test
    void classifiesNextFilesIntoBuildLayers() {
        assertThat(next("package.json", "{}")).isEqualTo(FileLayer.BUILD);
        assertThat(next("next.config.ts", "export default {}")).isEqualTo(FileLayer.BUILD);
        assertThat(next("prisma.config.ts", "export default defineConfig({})")).isEqualTo(FileLayer.BUILD);
        assertThat(next(".env.example", "DATABASE_URL=")).isEqualTo(FileLayer.CONFIG);
        assertThat(next("prisma/schema.prisma", "model Task {}")).isEqualTo(FileLayer.MIGRATION);
        assertThat(next("lib/validation.ts", "export const s = z.object({})")).isEqualTo(FileLayer.DOMAIN);
        assertThat(next("lib/db.ts", "export const prisma = new PrismaClient()")).isEqualTo(FileLayer.REPOSITORY);
        assertThat(next("app/actions.ts", "'use server';\nexport async function a() {}")).isEqualTo(FileLayer.SERVICE);
        assertThat(next("src/proxy.ts", "export function proxy() {}")).isEqualTo(FileLayer.SECURITY);
        assertThat(next("app/api/tasks/[id]/route.ts", "export async function GET() {}")).isEqualTo(FileLayer.CONTROLLER);
        assertThat(next("app/globals.css", "body {}")).isEqualTo(FileLayer.FRONTEND);
        assertThat(next("components/TaskItem.tsx", "'use client';")).isEqualTo(FileLayer.UI_COMPONENT);
        assertThat(next("app/tasks/[id]/page.tsx", "export default function Page() {}")).isEqualTo(FileLayer.UI_PAGE);
        assertThat(next("lib/validation.test.ts", "it('x', () => {})")).isEqualTo(FileLayer.TEST);

        assertThat(FileLayer.UI_PAGE.track()).isEqualTo(Track.FRONTEND);
        assertThat(FileLayer.CONTROLLER.label(ProjectStack.NEXTJS)).isEqualTo("API route handlers");
        assertThat(FileLayer.CONTROLLER.label(ProjectStack.SPRING)).isEqualTo(FileLayer.CONTROLLER.label());
    }

    @Test
    void importsDecideTheOrderInsideALayer() {
        List<Candidate> ordered = BuildOrderPlanner.order(List.of(
                file("app/page.tsx", "import TaskList from '@/components/TaskList';\nexport default function Home() {}"),
                file("components/TaskList.tsx", "import TaskItem from './TaskItem';\nexport default function TaskList() {}"),
                file("components/TaskItem.tsx", "'use client';\nimport { setStatusAction } from '../app/actions';"),
                file("lib/tasks.ts", "import { prisma } from './db';\nexport const all = () => prisma.task.findMany();"),
                file("lib/db.ts", "export const prisma = new PrismaClient();"),
                file("app/actions.ts", "'use server';\nimport { createTask } from '@/lib/tasks';"),
                file("package.json", "{}")));

        assertThat(ordered).extracting(Candidate::path).containsExactly(
                "package.json", "lib/db.ts", "lib/tasks.ts", "app/actions.ts",
                "components/TaskItem.tsx", "components/TaskList.tsx", "app/page.tsx");
    }

    // ------------------------------------------------------------------ explanations

    private static final List<String> ROUTE = """
            import { NextResponse, type NextRequest } from 'next/server';
            import { getTask } from '@/lib/tasks';

            export async function GET(_request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
              const { id } = await params;
              const task = await getTask(id);
              return NextResponse.json({ error: 'Task not found' }, { status: 404 });
            }
            """.lines().toList();

    private Explanation route(int n) {
        return explainer.explain(ROUTE, n, "typescript", FileLayer.CONTROLLER, "app/api/tasks/[id]/route.ts");
    }

    @Test
    void explainsRouteHandlersWithTheirUrl() {
        Explanation get = route(4);
        assertThat(get.kind()).isEqualTo("route-handler");
        assertThat(get.summary()).contains("GET /api/tasks/[id]");
        assertThat(get.why()).contains("405");

        assertThat(route(1).why()).contains("Line 7 uses NextResponse");
        assertThat(route(1).notes()).extracting(Note::term).contains("next/server");
        assertThat(route(5).notes()).extracting(Note::term).contains("await params");
        assertThat(route(6).notes()).extracting(Note::text).anyMatch(t -> t.contains("defined in lib/tasks (imported on line 2)"));
        assertThat(route(7).summary()).contains("HTTP 404");
        assertThat(route(6).context()).isEqualTo("inside GET()");

        var outline = explainer.outline(ROUTE, "typescript", FileLayer.CONTROLLER, "app/api/tasks/[id]/route.ts", ProjectStack.NEXTJS);
        assertThat(outline.purpose()).contains("/api/tasks/[id]");
        assertThat(outline.items()).extracting(i -> i.name()).contains("GET()");
    }

    @Test
    void explainsClientComponentsAndServerActions() {
        List<String> component = """
                'use client';

                export default function TaskForm() {
                  return <form action={formAction}>;
                }
                """.lines().toList();
        Explanation directive = explainer.explain(component, 1, "typescript", FileLayer.UI_COMPONENT, "components/TaskForm.tsx");
        assertThat(directive.kind()).isEqualTo("directive");
        assertThat(directive.summary()).contains("browser");

        List<String> actions = """
                'use server';
                export async function deleteTaskAction(id: string) {
                  revalidatePath('/');
                }
                """.lines().toList();
        Explanation action = explainer.explain(actions, 2, "typescript", FileLayer.SERVICE, "app/actions.ts");
        assertThat(action.kind()).isEqualTo("server-action");
        assertThat(explainer.explain(actions, 3, "typescript", FileLayer.SERVICE, "app/actions.ts").notes())
                .extracting(Note::term).contains("revalidatePath()");
    }

    @Test
    void explainsPrismaSchemaAndPackageJson() {
        List<String> schema = """
                model Task {
                  id        String   @id @default(cuid())
                  createdAt DateTime @default(now())
                }
                """.lines().toList();
        assertThat(explainer.explain(schema, 1, "prisma", FileLayer.MIGRATION, "prisma/schema.prisma").summary()).contains("Task");
        Explanation id = explainer.explain(schema, 2, "prisma", FileLayer.MIGRATION, "prisma/schema.prisma");
        assertThat(id.notes()).extracting(Note::term).contains("@id");
        assertThat(explainer.outline(schema, "prisma", FileLayer.MIGRATION, "prisma/schema.prisma", ProjectStack.NEXTJS).items())
                .extracting(i -> i.name()).containsExactly("Task");

        List<String> pkg = """
                {
                  "dependencies": {
                    "next": "16.0.0",
                    "zod": "^4.0.0"
                  }
                }
                """.lines().toList();
        Explanation nextDep = explainer.explain(pkg, 3, "json", FileLayer.BUILD, "package.json");
        assertThat(nextDep.summary()).contains("next");
        assertThat(nextDep.why()).isNotBlank();
    }

    @Test
    void springFrontendFilesKeepTheirOwnWording() {
        var outline = explainer.outline(List.of("export default function App() {}"), "typescript", FileLayer.FRONTEND,
                "frontend/src/App.tsx", ProjectStack.SPRING);
        assertThat(outline.purpose()).isEqualTo(FileLayer.FRONTEND.why());
    }
}
