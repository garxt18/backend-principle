package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.lab.LineExplainer.Explanation;
import com.backendprinciple.playground.lab.LineExplainer.Note;
import com.backendprinciple.playground.lab.LineExplainer.Outline;
import com.backendprinciple.playground.lab.LineExplainer.OutlineItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Explains TypeScript / JavaScript (React, Next.js App Router, zod, Prisma client), Prisma schema files and
 * package.json - line by line, with "what it does" and "why you type it". Same idea as the Java explainer:
 * patterns + a small knowledge base, no AI.
 */
final class ScriptExplainer {

    private static final Map<String, String> MODULES = new LinkedHashMap<>();
    private static final Map<String, String> APIS = new LinkedHashMap<>();
    private static final Map<String, String> PRISMA_ATTRS = new LinkedHashMap<>();

    static {
        MODULES.put("next/server", "Next.js server utilities: NextRequest / NextResponse for route handlers and proxy.ts.");
        MODULES.put("next/navigation", "App Router navigation: redirect(), notFound() on the server; useRouter(), usePathname(), useSearchParams() in client components.");
        MODULES.put("next/link", "<Link>: navigation between pages without a full page reload (and prefetching).");
        MODULES.put("next/image", "<Image>: automatically resized, lazy-loaded images.");
        MODULES.put("next/cache", "Next.js data cache: revalidatePath()/revalidateTag() throw away cached pages or data after a change.");
        MODULES.put("next/headers", "Read the incoming request's cookies() and headers() in server code.");
        MODULES.put("next/font", "Self-hosted web fonts with no layout shift.");
        MODULES.put("next", "Types from Next.js itself (Metadata, NextConfig...). Type imports disappear from the built JavaScript.");
        MODULES.put("react", "React: components, hooks (useState, useTransition, useActionState...) and types like ReactNode.");
        MODULES.put("react-dom", "React for the browser DOM (useFormStatus lives here).");
        MODULES.put("zod", "zod: declare the shape of data once, then validate unknown input against it and get TypeScript types for free.");
        MODULES.put("@prisma/client", "Prisma's database client.");
        MODULES.put("@prisma/adapter-pg", "Prisma 7 driver adapter: Prisma talks to PostgreSQL through the 'pg' driver.");
        MODULES.put("generated/prisma", "The Prisma client generated from schema.prisma by 'prisma generate' - fully typed for your models.");
        MODULES.put("prisma/config", "defineConfig for prisma.config.ts (Prisma 7 settings file).");
        MODULES.put("dotenv/config", "Loads the .env file into process.env as soon as it is imported.");
        MODULES.put("vitest", "Vitest test runner: describe/it/expect.");
        MODULES.put("vitest/config", "Vitest configuration helpers.");
        MODULES.put("node:url", "Node's URL helpers (fileURLToPath turns import.meta.url into a file path).");
        MODULES.put("drizzle-orm", "Drizzle ORM: SQL-like, typed queries.");
        MODULES.put("next-auth", "Auth.js (NextAuth): login with OAuth providers or credentials.");
        MODULES.put("bcryptjs", "Password hashing - never store plain passwords.");

        APIS.put("NextResponse.json", "Builds a JSON HTTP response (optionally with a status code and headers).");
        APIS.put("NextResponse.next", "Lets the request continue to the page or route it was going to.");
        APIS.put("NextResponse.redirect", "Answers with a redirect to another URL.");
        APIS.put("revalidatePath", "Marks the cached version of a page as stale so the next visit renders it again with fresh data.");
        APIS.put("revalidateTag", "Invalidates cached data tagged with this name.");
        APIS.put("redirect", "Stops rendering and sends the user to another URL.");
        APIS.put("notFound", "Stops rendering and shows the 404 page (not-found.tsx).");
        APIS.put("useState", "Local component state: [value, setValue]. Calling the setter re-renders the component.");
        APIS.put("useEffect", "Runs code after rendering (subscriptions, timers, fetching in client components).");
        APIS.put("useActionState", "React 19: wires a form to a server action and gives you [state returned by the action, formAction to put on <form action>, pending].");
        APIS.put("useTransition", "Runs an update (e.g. a server action) in the background: [pending, startTransition] keeps the UI responsive.");
        APIS.put("useFormStatus", "Tells a component inside a <form> whether that form is submitting.");
        APIS.put("useOptimistic", "Shows the expected result immediately while the server action is still running.");
        APIS.put("useRouter", "Programmatic navigation in client components: router.push('/x'), router.refresh().");
        APIS.put("useSearchParams", "Reads ?query=string values in client components.");
        APIS.put("usePathname", "The current URL path in client components.");
        APIS.put("useMemo", "Caches a computed value between renders.");
        APIS.put("useCallback", "Caches a function between renders.");
        APIS.put("useRef", "A mutable box that survives re-renders (often a DOM element reference).");
        APIS.put("safeParse", "zod: validates without throwing - returns { success: true, data } or { success: false, error }.");
        APIS.put("parse", "zod: validates and returns the typed data, or throws if the input is invalid.");
        APIS.put("z.object", "zod: an object with the listed fields and a rule for each.");
        APIS.put("z.enum", "zod: the value must be one of these strings.");
        APIS.put("z.infer", "Derives a TypeScript type from a zod schema - the type and the validation can never disagree.");
        APIS.put("z.coerce.date", "zod: converts the input (e.g. \"2026-10-01\") into a Date before checking it.");
        APIS.put("request.json", "Reads the request body and parses it as JSON (async).");
        APIS.put("formData.get", "Reads one field of a submitted <form> by its name attribute.");
        APIS.put("searchParams.get", "Reads one ?query parameter from the URL (null when missing).");
        APIS.put("headers.get", "Reads an HTTP request header.");
        APIS.put("findMany", "Prisma: SELECT the rows matching where/orderBy/take... and return them as an array.");
        APIS.put("findUnique", "Prisma: SELECT one row by a unique field (id or @unique); null when not found.");
        APIS.put("findFirst", "Prisma: the first row matching the filter, or null.");
        APIS.put("create", "Prisma: INSERT a row from data and return it (with generated id and timestamps).");
        APIS.put("update", "Prisma: UPDATE one row by a unique field - throws if it does not exist.");
        APIS.put("updateMany", "Prisma: UPDATE every row matching where; returns { count } and never throws for 0 rows.");
        APIS.put("delete", "Prisma: DELETE one row by a unique field - throws if it does not exist.");
        APIS.put("deleteMany", "Prisma: DELETE every row matching where; returns { count }.");
        APIS.put("upsert", "Prisma: update the row if it exists, otherwise create it.");
        APIS.put("count", "Prisma: SELECT count(*) for the filter.");
        APIS.put("startTransition", "Runs the given update as a transition; `pending` is true until it finishes.");
        APIS.put("toISOString", "Formats a Date as 2026-10-01T00:00:00.000Z.");

        PRISMA_ATTRS.put("@id", "primary key");
        PRISMA_ATTRS.put("@default(cuid())", "a new unique id is generated for every row (cuid)");
        PRISMA_ATTRS.put("@default(uuid())", "a random UUID is generated for every row");
        PRISMA_ATTRS.put("@default(autoincrement())", "the database numbers rows 1, 2, 3...");
        PRISMA_ATTRS.put("@default(now())", "set to the current time when the row is created");
        PRISMA_ATTRS.put("@updatedAt", "Prisma sets it to the current time on every update");
        PRISMA_ATTRS.put("@unique", "no two rows may have the same value (unique index)");
        PRISMA_ATTRS.put("@map", "the column is called differently in SQL (snake_case) than in TypeScript (camelCase)");
        PRISMA_ATTRS.put("@db.VarChar", "stored as VARCHAR with a maximum length");
        PRISMA_ATTRS.put("@db.Date", "stored as a DATE (no time of day)");
        PRISMA_ATTRS.put("@db.Text", "stored as TEXT");
        PRISMA_ATTRS.put("@relation", "a foreign key to another model");
        PRISMA_ATTRS.put("@default", "default value when none is given");
    }

    private static final Pattern IMPORT = Pattern.compile("^import\\s+(type\\s+)?(.*?)\\s*from\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern SIDE_IMPORT = Pattern.compile("^import\\s+['\"]([^'\"]+)['\"]");
    private static final Pattern FUNCTION = Pattern.compile(
            "^(export\\s+)?(default\\s+)?(async\\s+)?function\\s+(\\w+)\\s*(?:<[^>]*>)?\\s*\\(");
    private static final Pattern ARROW = Pattern.compile(
            "^(export\\s+)?const\\s+(\\w+)\\s*(?::[^=]+)?=\\s*(async\\s*)?\\(?[\\w\\s,{}:]*\\)?\\s*(?::\\s*[^=]+)?=>");
    private static final Pattern CONST = Pattern.compile("^(export\\s+)?(const|let|var)\\s+(\\[[^\\]]+]|\\{[^}]+}|\\w+)\\s*(?::\\s*[^=]+)?=\\s*(.*)$");
    private static final Pattern HTTP_METHOD = Pattern.compile("^export\\s+(async\\s+)?function\\s+(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\s*\\(");
    private static final Pattern CALL = Pattern.compile("([A-Za-z_][\\w.]*)\\s*\\(");
    private static final Pattern PRISMA_CALL = Pattern.compile("\\bprisma\\.(\\w+)\\.(\\w+)\\(");
    private static final Pattern ZOD_FIELD = Pattern.compile("^(\\w+):\\s*(z\\..*?),?$");
    private static final Pattern PROPERTY = Pattern.compile("^(\\w+|'[^']+'|\"[^\"]+\"):\\s*(.*[^;,{(\\[]),?$");
    private static final Pattern JSX_TAG = Pattern.compile("^<([A-Za-z][\\w.]*)");

    private ScriptExplainer() {
    }

    // ============================================================================ TypeScript / JavaScript

    static Explanation explainScript(List<String> lines, int n, String code, String t, FileLayer layer, String path) {
        String context = context(lines, n);
        String route = route(path);
        if (t.isEmpty()) {
            return new Explanation(n, code, "blank", "Blank line.", "Separates blocks so the file is easier to read.", context, List.of());
        }
        if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("*") || t.startsWith("{/*")) {
            return new Explanation(n, code, "comment", "Comment - ignored when the code runs.",
                    "Comments explain WHY the code is like this. Typing it makes you read the intent once more.", context, List.of());
        }
        if (t.matches("[})\\];,>/]+|\\)\\s*;?|}\\s*\\)\\s*;?|</?>")) {
            return new Explanation(n, code, "close", "Closes the block, call or JSX element opened above.",
                    "Every { ( [ and <tag> needs its closing partner; your editor's bracket highlighting shows which one.", context, List.of());
        }
        List<Note> notes = new ArrayList<>();
        String kind;
        String summary;
        String why;

        Matcher imp = IMPORT.matcher(t);
        Matcher side = SIDE_IMPORT.matcher(t);
        Matcher http = HTTP_METHOD.matcher(t);
        Matcher fn = FUNCTION.matcher(t);
        Matcher arrow = ARROW.matcher(t);
        Matcher cons = CONST.matcher(t);
        Matcher zodField = ZOD_FIELD.matcher(t);
        Matcher prisma = PRISMA_CALL.matcher(t);

        if (t.matches("['\"]use client['\"];?")) {
            kind = "directive";
            summary = "Marks this file as a Client Component: it is sent to the browser too, so it can use state, effects and event handlers.";
            why = "Components are Server Components by default. Without this line, useState / onClick here would fail - "
                    + "and everything this file imports also becomes client code, so keep such files small.";
        } else if (t.matches("['\"]use server['\"];?")) {
            kind = "directive";
            summary = "Every function exported from this file becomes a Server Action: the browser can call it (e.g. as a form action), but it always runs on the server.";
            why = "Without it these functions could not be called from client components, and they would never see the database or secrets.";
        } else if (imp.find()) {
            kind = "import";
            String names = imp.group(2).replaceAll("[{}]", "").strip();
            String module = imp.group(3);
            boolean typeOnly = imp.group(1) != null || imp.group(2).matches(".*\\btype\\s+\\w+.*") && !imp.group(2).contains(",");
            summary = (typeOnly ? "Imports only the TypeScript type(s) " : "Imports ") + names + " from '" + module + "'"
                    + (isLocal(module) ? " - a file of this project (" + module.replaceFirst("^[@~]/", "") + ")." : ".");
            String moduleText = moduleText(module);
            if (moduleText != null) {
                notes.add(new Note(module, moduleText));
            }
            why = importWhy(lines, n, names, typeOnly);
        } else if (side.find()) {
            kind = "import";
            String module = side.group(1);
            summary = module.endsWith(".css") ? "Loads the stylesheet " + module + (path.endsWith("layout.tsx") ? " once for the whole app." : ".")
                    : "Runs the module " + module + " for its side effects.";
            why = module.endsWith(".css") ? "CSS files are imported like code; the bundler adds them to the page. Global CSS belongs in the root layout."
                    : moduleText(module) != null ? moduleText(module) : "The module does something just by being loaded.";
        } else if (http.find()) {
            kind = "route-handler";
            String method = http.group(2);
            summary = "Handles HTTP " + method + " " + (route != null ? route : "requests to this route") + ". "
                    + (t.contains("params") ? "The second argument carries the [dynamic] URL segments." : "");
            why = "In app/**/route.ts, Next.js calls the exported function named after the HTTP method. A method with no function answers "
                    + "405 Method Not Allowed - so exporting " + method + " is what creates this endpoint.";
        } else if (t.startsWith("export function proxy") || t.startsWith("export function middleware")) {
            kind = "proxy";
            summary = "The proxy (formerly middleware): runs before every request whose path matches config.matcher below.";
            why = "One central check (auth, redirects, headers) instead of repeating it in every route. It must be exported from proxy.ts in the project root.";
        } else if (fn.find()) {
            String name = fn.group(4);
            boolean isDefault = fn.group(2) != null;
            boolean async = fn.group(3) != null;
            if (name.equals("generateMetadata")) {
                kind = "metadata";
                summary = "Builds this page's <title> and meta tags from its data (e.g. the task title).";
                why = "Next.js calls it before rendering the page; good titles matter for tabs, bookmarks and search engines.";
            } else if (isDefault && path.matches(".*(^|/)page\\.(tsx|jsx|js)$")) {
                kind = "page";
                summary = "The page for URL " + (route != null ? route : "this folder") + ": whatever it returns is what the user sees."
                        + (async ? " It is async, so it can await data (the database) while rendering on the server." : "");
                why = "In the App Router the folder is the URL and page.tsx must default-export a component - without it the URL is a 404.";
            } else if (isDefault && path.matches(".*(^|/)layout\\.(tsx|jsx|js)$")) {
                kind = "layout";
                summary = "A layout: wraps every page in this folder and below; {children} is where the page appears.";
                why = "Layouts stay mounted while you navigate between their pages, so shared UI (html, body, nav) is rendered once.";
            } else if (layer == FileLayer.SERVICE && t.startsWith("export async")) {
                kind = "server-action";
                summary = "Server action " + name + "(): runs on the server when the browser calls it.";
                why = "Forms and buttons can call it directly - no API route, no fetch code. Validate its input: anyone can call it.";
            } else if (Character.isUpperCase(name.charAt(0))) {
                kind = "component";
                summary = "React component <" + name + " />: a function that takes props and returns JSX (the UI)."
                        + (async ? " Async = a Server Component that awaits data." : "");
                why = "Components are the building blocks of the UI; capitalised names are how React tells components from HTML tags.";
            } else {
                kind = "function";
                summary = "Declares " + (async ? "the async function " : "the function ") + name + "()" + (t.startsWith("export") ? ", exported so other files can import it." : ".");
                why = functionWhy(layer, name, async);
            }
        } else if (arrow.find()) {
            kind = "function";
            summary = "Declares " + arrow.group(2) + " as an arrow function.";
            why = functionWhy(layer, arrow.group(2), arrow.group(3) != null);
        } else if (t.startsWith("export const metadata")) {
            kind = "metadata";
            summary = "Static metadata: the <title> and <meta description> for this layout/page.";
            why = "Next.js reads this exported object and writes the tags into <head> for you.";
        } else if (t.startsWith("export const dynamic")) {
            kind = "config";
            summary = "Route segment config: " + t.replaceAll("^export const dynamic\\s*=\\s*|;$", "") + ".";
            why = "'force-dynamic' renders the page on every request. Without it Next.js may pre-render it at build time, when the data does not exist yet.";
        } else if (t.startsWith("export const revalidate")) {
            kind = "config";
            summary = "Re-render this page at most every N seconds (incremental static regeneration).";
            why = "A middle ground between static (fast, stale) and dynamic (fresh, slower).";
        } else if (t.startsWith("export const config")) {
            kind = "config";
            summary = "Configuration object Next.js reads from this file (for proxy.ts: which paths it runs on).";
            why = "Limiting the matcher keeps the proxy away from pages and static files that do not need it.";
        } else if (t.startsWith("matcher:")) {
            kind = "config";
            summary = "The paths the proxy runs for: " + t.replaceFirst("matcher:\\s*", "").replaceAll(",$", "") + ".";
            why = ":path* matches everything below that path.";
        } else if (t.matches("^(export\\s+)?(type|interface)\\s+\\w+.*")) {
            kind = "type";
            summary = "Declares a TypeScript type: the shape of some data. Types exist only while coding - they are erased from the JavaScript.";
            why = t.contains("Promise<{") ? "In Next.js 15+ params arrive as a Promise, so the type says Promise<{ ... }> and the code must await it."
                    : "The compiler then checks every use: a typo in a field name is an error before the app even runs.";
        } else if (zodField.find()) {
            kind = "validation-rule";
            summary = "Validation rule for the field '" + zodField.group(1) + "': " + describeZod(zodField.group(2)) + ".";
            why = "Every request/form value passes these rules before it reaches the database; bad input gets a clear 400 instead of a crash.";
        } else if (prisma.find()) {
            kind = "query";
            String model = prisma.group(1);
            String op = prisma.group(2);
            summary = "Database query on the " + model + " table: " + APIS.getOrDefault(op, op + "()");
            why = "Prisma writes the SQL for you and the result is fully typed, so you cannot read a column that does not exist.";
        } else if (t.startsWith("where:") || t.startsWith("orderBy:") || t.startsWith("data:") || t.startsWith("select:")
                || t.startsWith("include:") || t.startsWith("take:") || t.startsWith("skip:")) {
            kind = "query";
            String key = t.substring(0, t.indexOf(':'));
            summary = switch (key) {
                case "where" -> "Filter: which rows the query touches (SQL WHERE).";
                case "orderBy" -> "Sort order of the results (SQL ORDER BY).";
                case "data" -> "The values to write.";
                case "select" -> "Only these columns are read.";
                case "include" -> "Also load these related rows (a JOIN).";
                default -> "Pagination: " + key + ".";
            };
            why = "Prisma query options are plain objects - TypeScript autocompletes every column name.";
        } else if (cons.find()) {
            kind = "variable";
            String name = cons.group(3);
            String value = cons.group(4);
            String hook = hookIn(value);
            if (hook != null) {
                summary = "Calls the hook " + hook + "(): " + APIS.get(hook);
                why = "Hooks must be called at the top level of a component, in the same order on every render.";
            } else if (value.startsWith("z.") || value.contains("Schema") && value.contains(".extend")) {
                summary = "Declares the zod schema " + name + ".";
                why = "One schema validates input everywhere (API routes, server actions, forms), and z.infer turns it into a TypeScript type.";
            } else {
                summary = "Declares " + (cons.group(2).equals("const") ? "the constant " : "the variable ") + name
                        + (value.isBlank() ? "." : " = " + JavaFileModel.shorten(value, 70));
                int use = firstUse(lines, name.replaceAll("[\\[\\]{}\\s]", "").split(",")[0], n);
                why = use > 0 ? "Line " + use + " uses it. `const` cannot be reassigned, which makes code easier to follow."
                        : "Holds the value for the lines below.";
            }
        } else if (t.startsWith("return")) {
            kind = "return";
            String rest = t.replaceFirst("^return\\s*", "").replaceAll(";$", "");
            summary = rest.startsWith("NextResponse.json") ? "Sends the JSON response" + statusOf(rest) + "."
                    : rest.startsWith("<") || rest.equals("(") ? "Returns JSX: the UI this component renders."
                    : rest.isEmpty() ? "Ends the function here." : "Returns " + JavaFileModel.shorten(rest, 70) + ".";
            why = rest.startsWith("NextResponse") ? "A route handler must return a Response; the status code tells the client what happened."
                    : rest.startsWith("<") || rest.equals("(") ? "A component's return value is its UI." : "The caller receives this value.";
        } else if (t.startsWith("if ") || t.startsWith("if(") || t.startsWith("} else")) {
            kind = "control";
            summary = t.startsWith("} else") ? "The other branch." : "Condition: the block runs only when " + t.replaceFirst("^if\\s*", "").replaceAll("\\{$", "").strip() + " is true.";
            why = "Early checks (not found, invalid input, missing key) keep the rest of the function simple.";
        } else if (t.startsWith("<") || t.startsWith("{") && t.endsWith("}")) {
            kind = "jsx";
            Matcher tag = JSX_TAG.matcher(t);
            String tagName = tag.find() ? tag.group(1) : null;
            summary = jsxSummary(t, tagName);
            why = jsxWhy(t, tagName);
        } else if (t.startsWith("await ") || t.contains("revalidatePath(") || t.contains("notFound(") || t.contains("redirect(")) {
            kind = "call";
            summary = "Runs " + JavaFileModel.shorten(t.replaceAll(";$", ""), 70) + (t.startsWith("await") ? " and waits for it to finish." : ".");
            why = t.startsWith("await ")
                    ? "await pauses this async function until the Promise settles, so the next line sees the finished result (or the error is thrown here)."
                    : "Called for its effect; the notes say what it does.";
        } else if (t.matches("^(describe|it|test)\\(.*")) {
            kind = "test";
            String name = t.replaceAll("^\\w+\\(\\s*['\"`](.*?)['\"`].*", "$1");
            summary = (t.startsWith("describe") ? "Groups the tests about " : "A test case: it ") + name + ".";
            why = t.startsWith("describe")
                    ? "describe() groups related tests so the report reads like a specification."
                    : "The test runner (vitest) calls this function; it fails if any expect() inside does not hold.";
        } else if (t.startsWith("expect(")) {
            kind = "assertion";
            summary = "Checks that " + JavaFileModel.shorten(t.replaceAll(";$", ""), 70) + ".";
            why = "An assertion: if the value is not what you expect, the test fails and shows both values.";
        } else if (t.matches("^export\\s+default\\s+\\w+\\s*;?$")) {
            kind = "export";
            summary = "Makes " + t.replaceAll("^export\\s+default\\s+|;$", "") + " the default export of this file.";
            why = "The tool that loads this file (Next.js, Prisma, vitest) imports its default export.";
        } else if (t.matches("^export\\s+default\\s+\\w+\\(.*")) {
            kind = "export";
            String defaultFn = t.replaceAll("^export\\s+default\\s+(\\w+)\\(.*", "$1");
            summary = "Exports the result of " + defaultFn + "(...) as this file's default export.";
            why = defaultFn.startsWith("define") ? defaultFn + "() only adds type checking: your editor autocompletes every option and flags typos."
                    : "The tool that loads this file imports its default export.";
        } else if (t.contains(".map(") && (t.startsWith("{") || context.contains("return"))) {
            kind = "jsx";
            summary = "Renders one element for every item of the array.";
            why = "JSX has no for-loop; mapping an array to elements is how React renders lists. Each element needs a key.";
        } else if (t.startsWith(".")) {
            kind = "chain";
            summary = "Continues the expression above with " + JavaFileModel.shorten(t.replaceAll("[;,]$", ""), 60) + ".";
            why = "Chained calls read top to bottom; each one works on the result of the previous line.";
        } else if (t.matches("^[\\w.\\[\\]'\"]+\\s*=\\s*[^=>].*;?$")) {
            kind = "assignment";
            summary = "Stores " + JavaFileModel.shorten(t.replaceAll("^.*?=\\s*|;$", ""), 50) + " in " + t.replaceAll("\\s*=.*", "") + ".";
            why = "Assignment changes a value that already exists (unlike const, which declares a new one).";
        } else if (t.matches("^[^<>{}()=;]+$") && context.contains("inside")) {
            kind = "jsx-text";
            summary = "Text shown inside the element above.";
            why = "Plain text between JSX tags is rendered as-is (React escapes it, so it cannot inject HTML).";
        } else if (PROPERTY.matcher(t).matches()) {
            Matcher prop = PROPERTY.matcher(t);
            prop.matches();
            kind = "property";
            summary = "Sets the property " + prop.group(1) + " to " + JavaFileModel.shorten(prop.group(2), 60) + ".";
            why = "Part of the object written above: each key: value pair becomes one property of it.";
        } else {
            kind = "statement";
            summary = "A statement of " + (context.isEmpty() ? "this file" : context.replace("inside ", "")) + ".";
            why = "Read it left to right: what is computed and where the result goes.";
        }

        // Notes: every known API on the line.
        Matcher call = CALL.matcher(t);
        while (call.find()) {
            String name = call.group(1);
            String shortName = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
            String text = APIS.containsKey(name) ? APIS.get(name)
                    : name.startsWith("prisma.") || name.startsWith("z.") ? APIS.get(shortName)
                    : APIS.containsKey(shortName) && !Character.isUpperCase(shortName.charAt(0)) && !kind.equals("import") ? APIS.get(shortName) : null;
            if (text != null && notes.stream().noneMatch(x -> x.text().equals(text))) {
                notes.add(new Note(shortName + "()", text));
            } else if (text == null && !name.contains(".") && !kind.equals("import")) {
                String local = localImport(lines, name);
                if (local != null && notes.stream().noneMatch(x -> x.term().equals(name + "()"))) {
                    notes.add(new Note(name + "()", local));
                }
            }
        }
        if (t.contains("await params") || t.contains("(await params)")) {
            notes.add(new Note("await params", "Next.js 15+ passes dynamic route params as a Promise; await it to get { id }."));
        }
        if (t.contains("?.")) {
            notes.add(new Note("?.", "Optional chaining: stops and gives undefined instead of crashing when the left side is null/undefined."));
        }
        if (t.contains("??")) {
            notes.add(new Note("??", "Nullish coalescing: use the right side only when the left side is null or undefined."));
        }
        if (t.contains("=>") && !kind.equals("function")) {
            notes.add(new Note("=>", "Arrow function: a short function, often passed as a callback."));
        }
        return new Explanation(n, code, kind, summary, why, context, notes.stream().limit(6).toList());
    }

    private static String functionWhy(FileLayer layer, String name, boolean async) {
        return switch (layer) {
            case REPOSITORY -> "Data-access functions are the only code that queries the database; pages, API routes and actions call them.";
            case SERVICE -> "Server-side logic lives in named functions so both server actions and API routes can reuse it.";
            case TEST -> "A test function: the runner calls it and reports it red if an expectation fails.";
            default -> async ? "async functions return a Promise; callers `await` the result." : "Named functions can be imported and tested on their own.";
        };
    }

    private static String jsxSummary(String t, String tag) {
        if (tag == null) {
            if (t.contains("&&")) {
                return "Conditional rendering: the part after && is shown only when the condition is true.";
            }
            if (t.contains(".map(")) {
                return "Renders one element per item of the array.";
            }
            if (t.equals("{children}")) {
                return "Where the page (or nested layout) is rendered inside this layout.";
            }
            return "Inserts a JavaScript value into the UI.";
        }
        return switch (tag) {
            case "form" -> t.contains("action=") ? "A form whose submit calls a server action directly (no fetch, no API route)." : "A form.";
            case "Link" -> "A link to another page; Next.js navigates without reloading the whole page.";
            case "Image" -> "An optimised image (resized, lazy-loaded).";
            case "button" -> t.contains("onClick") ? "A button with a click handler (this needs a client component)." : "A button.";
            case "input", "textarea", "select" -> "A form field; its name attribute is the key you read with formData.get().";
            case "html" -> "The <html> element - only the root layout renders it.";
            case "body" -> "The <body> element of every page.";
            default -> Character.isUpperCase(tag.charAt(0)) ? "Renders the component <" + tag + " /> here." : "The HTML element <" + tag + ">.";
        };
    }

    private static String jsxWhy(String t, String tag) {
        if (t.contains("key=")) {
            return "key lets React tell list items apart, so it updates only the item that changed.";
        }
        if (t.contains("className")) {
            return "JSX uses className (not class) because class is a reserved word in JavaScript.";
        }
        if ("form".equals(tag) && t.contains("action=")) {
            return "Works even before JavaScript loads (progressive enhancement), and the action receives the FormData.";
        }
        if (t.contains("onClick") || t.contains("onChange")) {
            return "Event handlers run in the browser - that is why this file starts with 'use client'.";
        }
        if (t.contains("disabled=")) {
            return "Disabling the button while pending stops double submissions.";
        }
        return "JSX looks like HTML but compiles to JavaScript function calls that build the UI.";
    }

    private static String describeZod(String chain) {
        List<String> parts = new ArrayList<>();
        Matcher m = Pattern.compile("\\.(\\w+)\\(([^()]*)\\)").matcher("." + chain.replaceFirst("^z\\.", ""));
        while (m.find()) {
            String op = m.group(1);
            String arg = m.group(2);
            parts.add(switch (op) {
                case "string" -> "a string";
                case "number" -> "a number";
                case "boolean" -> "true/false";
                case "trim" -> "spaces trimmed";
                case "min" -> "at least " + arg.split(",")[0].strip() + (chain.contains("string") ? " characters" : "");
                case "max" -> "at most " + arg.split(",")[0].strip() + (chain.contains("string") ? " characters" : "");
                case "optional" -> "may be left out";
                case "nullable" -> "may be null";
                case "email" -> "a valid email";
                case "date" -> "a date";
                case "int" -> "a whole number";
                case "positive" -> "greater than 0";
                default -> op;
            });
        }
        if (chain.contains("coerce")) {
            parts.addFirst("converted first (e.g. text -> Date)");
        }
        return parts.isEmpty() ? chain : String.join(", ", parts);
    }

    private static String statusOf(String rest) {
        Matcher m = Pattern.compile("status:\\s*(\\d{3})").matcher(rest);
        return m.find() ? " with HTTP " + m.group(1) : " (HTTP 200)";
    }

    private static String hookIn(String value) {
        Matcher m = Pattern.compile("\\b(use[A-Z]\\w*)\\s*(<[^>]*>)?\\(").matcher(value);
        return m.find() && APIS.containsKey(m.group(1)) ? m.group(1) : null;
    }

    /** "Defined in lib/tasks (imported on line 4)" when {@code name} is imported from this project. */
    private static String localImport(List<String> lines, String name) {
        Pattern word = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
        for (int i = 0; i < lines.size(); i++) {
            Matcher im = IMPORT.matcher(lines.get(i).strip());
            if (im.find() && isLocal(im.group(3)) && word.matcher(im.group(2)).find()) {
                return "Your own code: defined in " + im.group(3).replaceFirst("^@/", "") + " (imported on line " + (i + 1)
                        + "). Open that file to see what it does.";
            }
        }
        return null;
    }

    private static boolean isLocal(String module) {
        return module.startsWith("@/") || module.startsWith("~/") || module.startsWith("./") || module.startsWith("../");
    }

    private static String moduleText(String module) {
        if (module.contains("generated/prisma")) {
            return MODULES.get("generated/prisma");
        }
        String best = null;
        for (String key : MODULES.keySet()) {
            if ((module.equals(key) || module.startsWith(key + "/")) && (best == null || key.length() > best.length())) {
                best = key;
            }
        }
        return best == null ? null : MODULES.get(best);
    }

    private static String importWhy(List<String> lines, int n, String names, boolean typeOnly) {
        String first = names.replaceFirst("^type\\s+", "").split(",")[0].strip().replaceFirst("^type\\s+", "")
                .replaceAll("\\s+as\\s+\\w+", "");
        int use = firstUse(lines, first, n);
        String erased = typeOnly ? " A type-only import is erased at build time - it costs nothing at runtime." : "";
        if (use < 0) {
            return "Nothing below seems to use " + first + " - your editor would grey this import out." + erased;
        }
        return "Line " + use + " uses " + first + ": \"" + JavaFileModel.shorten(lines.get(use - 1).strip(), 60)
                + "\". Without the import TypeScript reports 'Cannot find name " + first + "'." + erased;
    }

    private static int firstUse(List<String> lines, String word, int afterLine) {
        if (word == null || word.isBlank() || !word.matches("[\\w$]+")) {
            return -1;
        }
        Pattern p = Pattern.compile("(?<![\\w$.])" + Pattern.quote(word) + "\\b");
        for (int j = afterLine; j < lines.size(); j++) {
            String l = lines.get(j).strip();
            if (!l.startsWith("import ") && !l.startsWith("//") && p.matcher(l).find()) {
                return j + 1;
            }
        }
        return -1;
    }

    /** "inside GET()" - the nearest function whose braces contain this line. */
    static String context(List<String> lines, int n) {
        int depth = 0;
        for (int i = n - 2; i >= 0; i--) {
            String l = lines.get(i).replaceAll("\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'", "\"\"");
            for (int c = l.length() - 1; c >= 0; c--) {
                char ch = l.charAt(c);
                if (ch == '}') {
                    depth++;
                } else if (ch == '{') {
                    depth--;
                }
            }
            if (depth < 0) {
                String t = lines.get(i).strip();
                Matcher f = FUNCTION.matcher(t);
                if (f.find()) {
                    return "inside " + f.group(4) + "()";
                }
                Matcher a = ARROW.matcher(t);
                if (a.find()) {
                    return "inside " + a.group(2) + "()";
                }
                depth = 0;
            }
        }
        return "";
    }

    /** app/api/tasks/[id]/route.ts -> /api/tasks/[id]; app/(shop)/cart/page.tsx -> /cart. */
    static String route(String path) {
        if (path == null) {
            return null;
        }
        String p = path.startsWith("src/") ? path.substring(4) : path;
        Matcher app = Pattern.compile("^app/(.*?)/?(page|route|layout)\\.(tsx|ts|jsx|js)$").matcher(p);
        if (p.matches("^app/(page|route|layout)\\.(tsx|ts|jsx|js)$")) {
            return "/";
        }
        if (app.find()) {
            String segments = app.group(1).replaceAll("\\([^)]*\\)/?", "").replaceAll("/$", "");
            return "/" + segments;
        }
        Matcher pages = Pattern.compile("^pages/(.*?)(/index)?\\.(tsx|ts|jsx|js)$").matcher(p);
        return pages.find() ? "/" + pages.group(1).replaceAll("^index$", "") : null;
    }

    static Outline outlineScript(List<String> lines, FileLayer layer, String path, ProjectStack stack) {
        List<OutlineItem> items = new ArrayList<>();
        String route = stack == ProjectStack.NEXTJS ? route(path) : null;
        String purpose = layer.why(stack);
        if (route != null && path.matches(".*route\\.(ts|js)$")) {
            purpose = "API endpoint " + route + ": each exported GET/POST/PATCH/DELETE function answers that HTTP method.";
        } else if (route != null && path.matches(".*page\\.(tsx|jsx|js)$")) {
            purpose = "The page at URL " + route + ".";
        } else if (route != null && path.matches(".*layout\\.(tsx|jsx|js)$")) {
            purpose = "Layout for " + route + " and every page below it.";
        }
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).strip();
            Matcher f = FUNCTION.matcher(t);
            Matcher a = ARROW.matcher(t);
            if (t.matches("['\"]use (client|server)['\"];?")) {
                items.add(new OutlineItem(i + 1, "directive", t.replaceAll("[;'\"]", ""), t.contains("client")
                        ? "Runs in the browser too (state, events)." : "Exports are server actions."));
            } else if (f.find() && (t.startsWith("export") || i == 0 || !lines.get(i).startsWith(" "))) {
                Explanation e = explainScript(lines, i + 1, lines.get(i), t, layer, path);
                items.add(new OutlineItem(i + 1, e.kind(), f.group(4) + "()", e.summary()));
            } else if (a.find() && t.startsWith("export")) {
                items.add(new OutlineItem(i + 1, "function", a.group(2) + "()", "Exported arrow function."));
            } else if (t.matches("^export\\s+(const|type|interface)\\s+\\w+.*")) {
                String name = t.replaceAll("^export\\s+(const|type|interface)\\s+(\\w+).*", "$2");
                items.add(new OutlineItem(i + 1, t.contains(" const ") ? "const" : "type", name,
                        explainScript(lines, i + 1, lines.get(i), t, layer, path).summary()));
            }
        }
        return new Outline(purpose, items);
    }

    // ============================================================================ Prisma schema

    static Explanation explainPrisma(List<String> lines, int n, String code, String t) {
        if (t.isEmpty()) {
            return new Explanation(n, code, "blank", "Blank line.", "Separates the blocks of the schema.", "", List.of());
        }
        if (t.startsWith("//")) {
            return new Explanation(n, code, "comment", "Comment.", "Ignored by Prisma.", "", List.of());
        }
        if (t.equals("}")) {
            return new Explanation(n, code, "close", "Closes the block.", "Every { needs its }.", "", List.of());
        }
        String block = prismaBlock(lines, n);
        if (t.startsWith("generator ")) {
            return new Explanation(n, code, "generator", "Configures `prisma generate`: which client to generate and where.",
                    "The generated client is what your TypeScript imports - typed for exactly these models.", "", List.of());
        }
        if (t.startsWith("datasource ")) {
            return new Explanation(n, code, "datasource", "Which database this schema is for.",
                    "Prisma writes SQL for this database; the connection URL itself lives in prisma.config.ts / .env.", "", List.of());
        }
        if (t.startsWith("model ")) {
            String name = t.split("\\s+")[1];
            return new Explanation(n, code, "model", "Declares the model " + name + ": one table in the database, one type in TypeScript.",
                    "`prisma migrate dev` turns it into CREATE TABLE, and `prisma generate` gives you prisma." + lowerFirst(name) + ".findMany() & co.",
                    "", List.of());
        }
        if (t.startsWith("enum ")) {
            return new Explanation(n, code, "enum", "Declares the enum " + t.split("\\s+")[1] + ": a fixed list of allowed values.",
                    "The database rejects any other value, and TypeScript autocompletes the allowed ones.", "", List.of());
        }
        if (t.startsWith("provider")) {
            String value = t.replaceAll(".*=\\s*", "").replace("\"", "");
            return new Explanation(n, code, "setting", block.startsWith("generator") ? "Generator: " + value + " (Prisma 7's TypeScript client)." : "Database engine: " + value + ".",
                    "Prisma adapts its SQL to this provider.", "", List.of());
        }
        if (t.startsWith("output")) {
            return new Explanation(n, code, "setting", "Where the generated client is written (" + t.replaceAll(".*=\\s*", "") + ").",
                    "Your code imports the client from this folder; it is generated, so keep it out of Git.", "", List.of());
        }
        if (t.startsWith("@@")) {
            String attr = t.substring(0, t.contains("(") ? t.indexOf('(') : t.length());
            String text = switch (attr) {
                case "@@index" -> "Creates a database index on these columns - fast WHERE/ORDER BY on them.";
                case "@@unique" -> "These columns together must be unique.";
                case "@@map" -> "The SQL table is called " + t.replaceAll(".*\\(\"?|\"?\\).*", "") + " (plural, snake_case) while the model keeps its TypeScript name.";
                case "@@id" -> "A primary key made of several columns.";
                default -> "A model-level attribute.";
            };
            return new Explanation(n, code, "attribute", text, "Model attributes shape the table, not a single column.", "", List.of());
        }
        if (block.startsWith("enum")) {
            return new Explanation(n, code, "enum-value", "Allowed value " + t + ".",
                    "Code can only store one of the listed values.", "", List.of());
        }
        String[] parts = t.split("\\s+", 3);
        if (parts.length >= 2) {
            String field = parts[0];
            String type = parts[1];
            boolean optional = type.endsWith("?");
            boolean list = type.endsWith("[]");
            String base = type.replaceAll("[?\\[\\]]", "");
            List<String> facts = new ArrayList<>();
            List<Note> notes = new ArrayList<>();
            if (parts.length == 3) {
                Matcher attr = Pattern.compile("@[\\w.]+(\\((?:[^()]|\\([^()]*\\))*\\))?").matcher(parts[2]);
                while (attr.find()) {
                    String a = attr.group();
                    String key = PRISMA_ATTRS.containsKey(a) ? a : a.replaceAll("\\(.*", "");
                    String text = PRISMA_ATTRS.get(key);
                    if (text != null) {
                        facts.add(text + (key.equals("@map") || key.startsWith("@db.VarChar") ? " (" + a.replaceAll(".*\\(|\\).*|\"", "") + ")" : ""));
                        notes.add(new Note(a, text));
                    }
                }
            }
            String sqlType = switch (base) {
                case "String" -> "TEXT";
                case "Int" -> "INTEGER";
                case "BigInt" -> "BIGINT";
                case "Float" -> "DOUBLE PRECISION";
                case "Decimal" -> "NUMERIC";
                case "Boolean" -> "BOOLEAN";
                case "DateTime" -> "TIMESTAMP";
                case "Json" -> "JSONB";
                default -> null;
            };
            String summary = "Column " + field + " of type " + base + (sqlType != null ? " (" + sqlType + " in SQL)" : " (another model or enum)")
                    + (optional ? ", may be empty (NULL)" : list ? ", a list (the other side of a relation)" : ", required")
                    + (facts.isEmpty() ? "." : ": " + String.join("; ", facts) + ".");
            String why = optional ? "The ? makes it optional in both the database and the TypeScript type (string | null)."
                    : "Required fields become NOT NULL columns and non-optional TypeScript properties.";
            return new Explanation(n, code, "field", summary, why, "inside " + block, notes);
        }
        return new Explanation(n, code, "prisma", "Prisma schema line.", "Part of the data model.", "", List.of());
    }

    private static String prismaBlock(List<String> lines, int n) {
        for (int i = n - 2; i >= 0; i--) {
            String t = lines.get(i).strip();
            if (t.equals("}")) {
                return "";
            }
            if (t.endsWith("{")) {
                return t.replace("{", "").strip();
            }
        }
        return "";
    }

    static Outline outlinePrisma(List<String> lines) {
        List<OutlineItem> items = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).strip();
            if (t.matches("^(model|enum|generator|datasource)\\s+\\w+.*")) {
                items.add(new OutlineItem(i + 1, t.split("\\s+")[0], t.split("\\s+")[1], explainPrisma(lines, i + 1, lines.get(i), t).summary()));
            }
        }
        return new Outline("The data model: every model becomes a table and a TypeScript type.", items);
    }

    // ============================================================================ package.json / tsconfig

    static Explanation explainJson(List<String> lines, int n, String code, String t, String path) {
        if (t.isEmpty() || t.matches("[{}\\[\\],]+")) {
            return new Explanation(n, code, "close", t.isEmpty() ? "Blank line." : "JSON bracket.", "JSON must be perfectly balanced - no trailing commas, double quotes only.", "", List.of());
        }
        Matcher kv = Pattern.compile("^\"([^\"]+)\"\\s*:\\s*(.*?),?$").matcher(t);
        if (!kv.find()) {
            return new Explanation(n, code, "json", "JSON value.", "Part of the configuration.", "", List.of());
        }
        String key = kv.group(1);
        String value = kv.group(2);
        String section = jsonSection(lines, n);
        boolean pkg = path != null && path.endsWith("package.json");
        String summary;
        String why;
        List<Note> notes = new ArrayList<>();
        if (pkg && (section.equals("dependencies") || section.equals("devDependencies"))) {
            String version = value.replace("\"", "");
            summary = (section.equals("dependencies") ? "Runtime dependency " : "Development-only tool ") + key + " " + version
                    + (version.startsWith("^") ? " (^ = any later " + version.substring(1).split("\\.")[0] + ".x.x version is fine)" : "") + ".";
            String text = moduleText(key);
            if (text != null) {
                notes.add(new Note(key, text));
            }
            why = section.equals("dependencies") ? "npm install downloads it into node_modules; the app needs it when running."
                    : "Only needed to build/test (compilers, type definitions, CLIs) - not shipped with the app.";
        } else if (pkg && section.equals("scripts")) {
            summary = "Script `npm run " + key + "` runs: " + value.replace("\"", "") + ".";
            why = switch (key) {
                case "dev" -> "Starts the development server with hot reload.";
                case "build" -> "Creates the optimised production build (here: generate the Prisma client first).";
                case "start" -> "Runs the production build.";
                case "test" -> "Runs the automated tests once.";
                default -> "Scripts give the team one short, shared command for each task.";
            };
        } else if (pkg && value.endsWith("{")) {
            summary = "Starts the \"" + key + "\" section.";
            why = switch (key) {
                case "scripts" -> "Named commands: npm run <name>.";
                case "dependencies" -> "Libraries the app needs at runtime.";
                case "devDependencies" -> "Tools needed only while developing, building and testing.";
                default -> "Groups related settings.";
            };
        } else if (pkg) {
            summary = "Package setting \"" + key + "\" = " + value + ".";
            why = switch (key) {
                case "private" -> "true = npm refuses to publish it by accident.";
                case "type" -> "\"module\" = files use import/export (ES modules).";
                case "name", "version" -> "Identifies the project.";
                default -> "A package.json field.";
            };
        } else {
            summary = "Setting \"" + key + "\" = " + value + ".";
            why = switch (key) {
                case "strict" -> "Turns on all of TypeScript's strict checks (null safety etc.) - catches most bugs at compile time.";
                case "paths" -> "Lets you write @/lib/db instead of ../../lib/db.";
                case "jsx" -> "How JSX is compiled.";
                case "noEmit" -> "TypeScript only type-checks; Next.js (SWC) does the actual compiling.";
                case "moduleResolution" -> "\"bundler\" resolves imports the way Next.js' bundler does.";
                case "plugins" -> "The Next.js plugin adds Next-specific type checks in the editor.";
                default -> "A compiler/tool option.";
            };
        }
        return new Explanation(n, code, "config", summary, why, section.isEmpty() ? "" : "inside \"" + section + "\"", notes);
    }

    private static String jsonSection(List<String> lines, int n) {
        int depth = 0;
        for (int i = n - 2; i >= 0; i--) {
            String t = lines.get(i).strip();
            if (t.startsWith("}")) {
                depth++;
            }
            if (t.endsWith("{")) {
                if (depth == 0) {
                    Matcher m = Pattern.compile("^\"([^\"]+)\"").matcher(t);
                    return m.find() ? m.group(1) : "";
                }
                depth--;
            }
        }
        return "";
    }

    private static String lowerFirst(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toLowerCase(Locale.ROOT) + s.substring(1);
    }
}
