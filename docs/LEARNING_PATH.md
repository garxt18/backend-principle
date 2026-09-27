# Your Learning Path

> learn -> build -> break -> debug -> improve -> repeat

This guide turns the *Backend Engineering Roadmap 2026* into a routine. The same data (with progress tracking)
lives in the app under **Roadmap** and **Planly**, and is generated from `src/main/resources/roadmap/roadmap.json`.

## Your two playlists (the only resources for Java and Spring Boot)

| Levels | Playlist | Lectures |
|---|---|---|
| L0 Java Basics, L2 Advanced Java | [Java Full Course 2026 - Coder Army](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (Hindi) | 1-21, 22-57 |
| L3 Spring Boot | [Spring Boot Full Course 2026](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (Hindi) | 1-40 |

Each lecture is one topic in the roadmap. **watch** = the exact video was found. **playlist** = the exact
video could not be found automatically, so the link opens the playlist - use **+ My link** on that topic in the
app to save the exact video once you find it. Lecture topics come from the course's own code repositories.

### How to combine a lecture with the Rebuild Lab

1. **Watch** the lecture (no coding yet), taking 3-5 bullet notes in the topic's notes box.
2. **Rebuild**: open the matching file in the Rebuild Lab and type it in Type mode. Under the line you are typing,
   read *What this line does* and *Why you type it*; then write your own explanation in "Explain it in your own words".
3. **Recall**: retype the same file in **Blind mode**. Wherever you get stuck is what to rewatch.
4. **Break it**: change something in your own project (remove `@Transactional`, rename a repository method,
   drop an index) and predict what happens before you run it.
5. **Log** the time on the dashboard and mark the lecture done (Planly updates automatically).

Want a second stack? The **Next.js Task Board** template is the same CRUD idea as the Task Manager API, built
the Next.js way: Prisma schema -> data access in `lib/` -> server actions -> `route.ts` handlers -> components ->
pages. Rebuild it after L3 to see which backend ideas (validation, layering, 404/400 handling) carry across stacks.

Away from the website? On the project page use **Download progress**, keep typing in IntelliJ/VS Code, then zip
the folder and **Sync from computer**.

### Daily routine

- Create a plan in **Planly** with your real hours (20 h/week ~ 6 months).
- Every day: one roadmap topic/lecture + 1-2 problems from [Striver's A2Z sheet](https://takeuforward.org/dsa/strivers-a2z-sheet-learn-dsa-a-to-z)
  (linked on the dashboard; it tracks solved problems itself, so there is no copy of it in this app).
- Sunday: revise, and re-plan if you fell behind - that is what the plan is for.

Suggested order in the Lab: **Task Manager API** template (a weekend) -> **your own project** -> the
**Backend Playground** template (advanced: JWT, rate limiting, caching).

## 6-month plan (about 20 hours per week)

| Month | Focus | Goal |
|---|---|---|
| 1 | Java Basics + Git + Advanced Java (L0-L2) + DSA on A2Z | Finish Java lectures 1-57, get comfortable with the CLI |
| 2 | Spring Boot + REST + PostgreSQL (L3-L5) | Spring lectures 1-40, first CRUD APIs on a real database; rebuild your project |
| 3 | Security + Testing (L6-L7) | Add JWT, write JUnit/Mockito/Testcontainers tests |
| 4 | Redis + Kafka + Microservices (L8-L10) | Caching, events, service boundaries |
| 5 | Docker + AWS + Kubernetes + CI/CD + Observability (L12-L14) | Containerise, deploy, monitor |
| 6 | System Design + Projects + Interview prep (L11, L15, bonus Spring AI) | Capstone food-delivery backend, mock interviews |

## L0 Java Basics - lectures 1-21

| # | Lecture | What it teaches | Link |
|---|---|---|---|
| 1 | Introduction to Java | What Java is, where it is used (backend, Android, big data), why it is platform independent and how this course is structured. | [watch](https://www.youtube.com/watch?v=LBqE4YOvhyc) |
| 2 | First Java program, JVM vs JDK vs JRE | Writing and running Hello World; compile (javac) vs run (java); bytecode; what the JVM, JRE and JDK each do; Java SE vs EE vs ME. | [watch](https://www.youtube.com/watch?v=pdS8_smlsXA) |
| 3 | Variables, data types, identifiers, literals, keywords | Primitive types (byte..double, char, boolean), reference types, naming rules, literals, reserved keywords. | [watch](https://www.youtube.com/watch?v=NtmULLvsABc) |
| 4 | How Java stores negative & floating-point numbers | Binary, two's complement for negatives, IEEE-754 floats, why 0.1 + 0.2 != 0.3, overflow. | [watch](https://www.youtube.com/watch?v=iV8hy9DdKFM) |
| 5 | Type conversion & type promotion | Implicit widening, explicit narrowing casts, promotion in expressions (byte + byte = int), data loss. | [watch](https://www.youtube.com/watch?v=QpWha2cLS1c) |
| 6 | Operators | Arithmetic, relational, logical, bitwise, shift, assignment, ternary, increment/decrement, precedence. | [watch](https://www.youtube.com/watch?v=Tdq4fmDCJH8) |
| 7 | Conditional statements | if / else-if / else, nested conditions, switch (classic and arrow syntax / switch expressions). | [watch](https://www.youtube.com/watch?v=cdNhXVb5j7o) |
| 8 | Loops & jump statements | for, while, do-while, for-each, break, continue, labelled break, pattern printing. | [watch](https://www.youtube.com/watch?v=d6hNDJrj1bw) |
| 9 | Arrays & introduction to Strings | Declaring/initialising arrays, default values, traversal, 2D arrays basics, String basics. | [watch](https://www.youtube.com/watch?v=ajNs-dylWIM) |
| 10 | Arrays & Strings in practice | More array problems and String handling from the playlist's notes (link not auto-found - open the playlist at lecture 10). | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 10) |
| 11 | Functions deep dive, recursion, overloading | Method signature, parameters vs arguments, return types, call stack, recursion, method overloading. | [watch](https://www.youtube.com/watch?v=IhtjMWXwNCc) |
| 12 | OOP: classes, objects & the new keyword | Why OOP, class vs object, fields and methods, what `new` does in memory. | [watch](https://www.youtube.com/watch?v=tmXD7Kzun54) |
| 13 | Constructors | Default, parameterised and copy constructors, constructor chaining with this(), initialisation order. | [watch](https://www.youtube.com/watch?v=RADpqk79IzQ) |
| 14 | Objects deep dive: call by value, shallow vs deep copy | Java is always pass-by-value (of references), aliasing, shallow vs deep copy. | [watch](https://www.youtube.com/watch?v=7yd5qO2TPr0) |
| 15 | static & final, String[] args | Static fields/methods/blocks, final variables/methods/classes, command-line arguments. | [watch](https://www.youtube.com/watch?v=Lyg4ZcrjUBw) |
| 16 | Encapsulation, inheritance, packages & super | Access modifiers, getters/setters, extends, method overriding, super, packages & imports. | [watch](https://www.youtube.com/watch?v=jfLpzL1VW7Q) |
| 17 | Abstraction & polymorphism, abstract class vs interface | Compile-time vs runtime polymorphism, abstract classes, interfaces, when to use which. | [watch](https://www.youtube.com/watch?v=-L9BcU6Xk2c) |
| 18 | One public class per file, wrapper classes & autoboxing | Why a file has one public class, wrapper types, autoboxing/unboxing, Integer cache pitfalls. | [watch](https://www.youtube.com/watch?v=crVwHNW-fX8) |
| 19 | Nested classes | Static nested, inner, local and anonymous classes; when each is useful. | [watch](https://www.youtube.com/watch?v=0LaBK28_470) |
| 20 | Java I/O deep dive | Streams vs readers/writers, File, BufferedReader/Writer, try-with-resources, Scanner. | [watch](https://www.youtube.com/watch?v=yfMWLZGEPwo) |
| 21 | Immutable classes | Rules for immutability, final fields, defensive copies, why String is immutable, records. | [watch](https://www.youtube.com/watch?v=vgQVz5OdFws) |


## L2 Advanced Java - lectures 22-57

| # | Lecture | What it teaches | Link |
|---|---|---|---|
| 22 | The Object class | equals, hashCode, toString, the equals/hashCode contract, getClass, clone. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 22) |
| 23 | Enums | Enum constants, fields/constructors in enums, switch on enums, values(), EnumMap basics. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 23) |
| 24 | Interfaces in depth | Default & static methods, multiple inheritance of type, the diamond problem, functional interfaces. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 24) |
| 25 | String pool, immutability & internals | String pool, new String vs literal, intern(), == vs equals, why Strings are immutable. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 25) |
| 26 | String methods, StringBuilder & StringBuffer | Common String API, StringBuilder vs StringBuffer, performance of concatenation in loops. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 26) |
| 27 | Type casting of objects & introduction to Generics | Upcasting/downcasting, instanceof (with pattern matching), why generics exist, generic classes. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 27) |
| 28 | Wildcards in Generics | ? extends, ? super, PECS rule, unbounded wildcards. | [watch](https://www.youtube.com/watch?v=Opb2q2WSyes) |
| 29 | Generics in depth | Bounded types, generic methods, type erasure and its limits (notes-only lecture - open the playlist at lecture 29). | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 29) |
| 30 | Iterable interface | How for-each works, Iterator, implementing Iterable for your own collection. | [watch](https://www.youtube.com/watch?v=LVwpxEZ6uMQ) |
| 31 | Collection interface | The Collections Framework hierarchy, Collection methods, Collections utility class. | [watch](https://www.youtube.com/watch?v=cazzzwdLAkQ) |
| 32 | List interface | ArrayList vs LinkedList internals, time complexity, ListIterator, common pitfalls. | [watch](https://www.youtube.com/watch?v=OfNtIOGDqzY) |
| 33 | Set & Map interfaces | HashSet/LinkedHashSet/TreeSet, HashMap/LinkedHashMap/TreeMap, how hashing works. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 33) |
| 34 | Set & Map methods, EnumMap, IdentityHashMap | Map API (getOrDefault, merge, computeIfAbsent), EnumMap, IdentityHashMap, WeakHashMap. | [watch](https://www.youtube.com/watch?v=b1Uj_2MPBwA) |
| 35 | Queue interface, PriorityQueue & heap | Queue/Deque, ArrayDeque, PriorityQueue as a binary heap, custom ordering. | [watch](https://www.youtube.com/watch?v=zj0-JIc5oPw) |
| 36 | Comparable | Natural ordering, compareTo contract, sorting custom objects. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 36) |
| 37 | Comparator & lambda basics | Comparator, comparing().thenComparing(), reversed(), lambda syntax. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 37) |
| 38 | Lambdas & functional interfaces | Function, Predicate, Consumer, Supplier, method references, effectively final. | [watch](https://www.youtube.com/watch?v=IsC0Ms2iFi8) |
| 39 | Introduction to Streams | What a stream is, source -> intermediate -> terminal, laziness, filter/map/forEach. | [watch](https://www.youtube.com/watch?v=-jHyDEXHr_U) |
| 40 | Stream operations & collectors | flatMap, sorted, distinct, reduce, collect, groupingBy, partitioningBy, joining. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 40) |
| 41 | Optional | Why null is dangerous, Optional creation, map/orElse/orElseThrow, when not to use Optional. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 41) |
| 42 | Parallel streams | parallelStream, forEachOrdered, Spliterator, when parallel streams are slower. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 42) |
| 43 | Exception handling from scratch | Checked vs unchecked, try/catch/finally, multi-catch, try-with-resources. | [watch](https://www.youtube.com/watch?v=mJg9iQZJEpw) |
| 44 | Exception hierarchy, throw vs throws, custom exceptions | Throwable/Error/Exception tree, throw vs throws, creating custom exceptions, chaining. | [watch](https://www.youtube.com/watch?v=xTxscoajikw) |
| 45 | Memory management: stack, heap, method area & PC | JVM memory areas, what lives where, object lifecycle, StackOverflowError vs OutOfMemoryError. | [watch](https://www.youtube.com/watch?v=kjETbH63Pco) |
| 46 | Garbage collection | How the JVM frees memory, reachability, generations (young/old), GC roots (open the playlist at lecture 46). | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 46) |
| 47 | Multithreading intro: process vs thread | Concurrency vs parallelism, process vs thread, why backends are multithreaded. | [watch](https://www.youtube.com/watch?v=fyAW0W526RM) |
| 48 | Thread creation & lifecycle | Extending Thread vs Runnable, start vs run, thread states. | [watch](https://www.youtube.com/watch?v=cVRdeQFP5IM) |
| 49 | Thread methods | sleep, join, yield, interrupt, daemon threads, priorities. | [watch](https://www.youtube.com/watch?v=ZPxJby0GeOQ) |
| 50 | Problems in multithreading | Race conditions, visibility problems, deadlock, livelock, starvation. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 50) |
| 51 | Synchronization | synchronized methods/blocks, intrinsic locks, volatile, happens-before. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 51) |
| 52 | Inter-thread communication | wait/notify/notifyAll, producer-consumer problem. | [watch](https://www.youtube.com/watch?v=EZS19NLnsvc) |
| 53 | Java locks | ReentrantLock, tryLock, fairness, ReadWriteLock, Condition. | [watch](https://www.youtube.com/watch?v=JW6-TCU0iS4) |
| 54 | Lock-free concurrency (atomics) | Why count++ is not atomic, AtomicInteger/AtomicLong/AtomicReference. | [playlist](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (open lecture 54) |
| 55 | Lock-free concurrency 2: CAS & the ABA problem | Compare-and-swap, spin loops, ABA problem, AtomicStampedReference. | [watch](https://www.youtube.com/watch?v=2nBJPpERul4) |
| 56 | Executor framework | Thread pools, ExecutorService, Callable/Future, fixed vs cached pools, shutdown. | [watch](https://www.youtube.com/watch?v=VPtaTUSaBOM) |
| 57 | CompletableFuture, Fork-Join, ThreadLocal, virtual threads | Async pipelines, thenApply/thenCombine, ForkJoinPool, ThreadLocal, Java 21 virtual threads. | [watch](https://www.youtube.com/watch?v=FGN225TiXaE) |


## L3 Spring Boot - lectures 1-40

| # | Lecture | What it teaches | Link |
|---|---|---|---|
| 1 | Spring Framework & Spring Boot introduction | Why Spring was created, the problems of plain Java EE, what Spring Boot adds on top. | [watch](https://www.youtube.com/watch?v=Q04sTEwC7Kg) |
| 2 | Your first Spring Boot app & REST API | start.spring.io, project structure, @SpringBootApplication, a first @RestController endpoint. | [watch](https://www.youtube.com/watch?v=PVreyET_Q6I) |
| 3 | Apache Maven | pom.xml, dependencies, scopes, lifecycle (compile/test/package), the Maven wrapper. | [watch](https://www.youtube.com/watch?v=N2EXGMJVwUU) |
| 4 | Dependency Injection & IoC | Tight vs loose coupling, inversion of control, constructor/setter/field injection. | [watch](https://www.youtube.com/watch?v=dT10m_5POfs) |
| 5 | IoC container, beans, @Component, @Autowired & @Bean | ApplicationContext, bean definitions, component scanning, @Bean methods. | [watch](https://www.youtube.com/watch?v=Az9tIgCdaRU) |
| 6 | Circular dependency, bean scope, lazy & eager | singleton vs prototype (and web scopes), @Lazy, why circular dependencies fail. | [watch](https://www.youtube.com/watch?v=ass8cR52Cf8) |
| 7 | Bean lifecycle | Instantiation -> DI -> init -> use -> destroy, @PostConstruct, @PreDestroy, BeanPostProcessor. | [watch](https://www.youtube.com/watch?v=WXg3uyZPqYI) |
| 8 | Spring XML configuration | Configuring beans in XML (the old way) to understand what annotations replace. | [watch](https://www.youtube.com/watch?v=HqBh9hV7rik) |
| 9 | Spring Boot core: @SpringBootApplication & auto-configuration | Java/annotation config (@Configuration), what @SpringBootApplication combines, how auto-configuration decides what to create. | [watch](https://www.youtube.com/watch?v=a06KMY209AA) |
| 10 | application.properties, @Value & runner interfaces | Externalised config, @Value, @ConfigurationProperties, CommandLineRunner / ApplicationRunner. | [watch](https://www.youtube.com/watch?v=4naCY3vCSfk) |
| 11 | CRUD project from scratch: API & architecture | Controller -> Service -> Repository layers, designing endpoints for a Student API. | [watch](https://www.youtube.com/watch?v=6kCCIdLLGRs) |
| 12 | CRUD with a real database: Spring Data JPA methods | Connecting MySQL/Postgres, @Entity, JpaRepository save/findAll/findById/deleteById. | [watch](https://www.youtube.com/watch?v=uRr8_0x_HYw) |
| 13 | CRUD part 3: PATCH & soft delete | Partial updates with PATCH, soft delete with an active flag instead of deleting rows. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 13) |
| 14 | How Java web apps work: Servlets, Tomcat & WAR files | Servlet API, servlet container, request/response lifecycle, WAR vs executable JAR. | [watch](https://www.youtube.com/watch?v=TSHrq65q5dw) |
| 15 | Spring MVC architecture: DispatcherServlet & HandlerMapping | Front controller pattern, DispatcherServlet, HandlerMapping, view resolution (JSP flow). | [watch](https://www.youtube.com/watch?v=M7_XxF6Zczw) |
| 16 | DTOs & validation | Why not expose entities, request/response DTOs, Bean Validation (@Valid, @NotBlank, @Size). | [watch](https://www.youtube.com/watch?v=vXA78rtCeTY) |
| 17 | Exception handling for professional APIs | ResponseEntity & status codes, @ExceptionHandler, @RestControllerAdvice, error response body. | [watch](https://www.youtube.com/watch?v=1au7XK_b1sI) |
| 18 | Profiles & YAML: dev vs prod config | application.yml, profile-specific files, spring.profiles.active, environment variables. | [watch](https://www.youtube.com/watch?v=K3H-rqhndEM) |
| 19 | Filters: FilterChain, logging, auth & request flow | Servlet filters, OncePerRequestFilter, filter order, logging and auth filters. | [watch](https://www.youtube.com/watch?v=ma-macdL9DI) |
| 20 | Filters part 2: registration, ordering & response filters | FilterRegistrationBean, URL patterns, ordering multiple filters, modifying responses/headers. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 20) |
| 21 | Interceptors: preHandle, postHandle & afterCompletion | HandlerInterceptor, WebMvcConfigurer, filter vs interceptor. | [watch](https://www.youtube.com/watch?v=XE4xsgbArCQ) |
| 22 | AOP: cross-cutting concerns & proxies | What AOP solves, aspects, join points, advice, how Spring uses proxies. | [watch](https://www.youtube.com/watch?v=s4B-WbUWEFs) |
| 23 | AOP advice types: @Before, @After, @Around | All advice types, when each executes, ProceedingJoinPoint. | [watch](https://www.youtube.com/watch?v=hMsALeV_uG0) |
| 24 | AOP pointcuts & types of proxies | Pointcut expressions (execution, within, @annotation), JDK dynamic proxy vs CGLIB. | [watch](https://www.youtube.com/watch?v=Kf2V2QYikVY) |
| 25 | Custom annotations with AOP | Creating @LogExecutionTime-style annotations and binding them in advice. | [watch](https://www.youtube.com/watch?v=TT-b9ob-lN8) |
| 26 | JDBC from scratch | DriverManager, Connection, PreparedStatement, ResultSet, SQL injection, closing resources. | [watch](https://www.youtube.com/watch?v=IbCHISQOMUA) |
| 27 | Spring JDBC: JdbcTemplate, DataSource & HikariCP | JdbcTemplate, RowMapper, DataSource, connection pooling with HikariCP. | [watch](https://www.youtube.com/watch?v=OPjnbYbcb2E) |
| 28 | Hibernate fundamentals: CRUD & entity mapping | ORM idea, @Entity/@Table/@Id/@GeneratedValue/@Column, Session vs EntityManager. | [watch](https://www.youtube.com/watch?v=XVQ27D8IIWU) |
| 29 | Hibernate internals: persistence context, L1 cache & transactions | Entity states, dirty checking, first-level cache, flush, why updates happen without save(). | [watch](https://www.youtube.com/watch?v=0Ko1Ux1otqE) |
| 30 | JPA relationships part 1 | @OneToOne and @ManyToOne/@OneToMany, owning side, @JoinColumn, fetch types. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 30) |
| 31 | JPA relationships part 2 | Bidirectional mappings, mappedBy, cascade, orphanRemoval, @ManyToMany, N+1 problem. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 31) |
| 32 | Spring Data JPA: JpaRepository, JPQL & pagination | Derived query methods, @Query (JPQL/native), Pageable & Sort. | [watch](https://www.youtube.com/watch?v=xugZno16FgQ) |
| 33 | Transactions part 1: @Transactional | ACID, @Transactional on service methods, rollback rules, a money transfer example. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 33) |
| 34 | Transactions part 2: propagation & isolation | REQUIRED vs REQUIRES_NEW (audit logs), isolation levels, self-invocation pitfall. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 34) |
| 35 | Spring Security: authentication, authorization & SecurityContext | Security filter chain, Authentication, AuthenticationManager, SecurityContextHolder. | [watch](https://www.youtube.com/watch?v=KVJuG95JSpM) |
| 36 | Spring Security practical: users & roles in the database | User/Role entities, password hashing with BCrypt, registration endpoint, SecurityConfig. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 36) |
| 37 | UserDetailsService & role-based access | CustomUserDetails, CustomUserDetailsService, hasRole/hasAuthority rules. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 37) |
| 38 | JWT authentication | Login endpoint, generating and validating JWTs, a JWT filter, stateless sessions. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 38) |
| 39 | OAuth 2.0 & OpenID Connect: Login with Google | OAuth2 roles and flows, OIDC, PKCE, spring-boot-starter-oauth2-client. | [watch](https://www.youtube.com/watch?v=t9kRN4wMVk0) |
| 40 | Testing Spring Boot apps | Unit tests with JUnit 5 & Mockito, @WebMvcTest, @DataJpaTest, @SpringBootTest. | [playlist](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (open lecture 40) |


## Resources for the other levels

HI = Hindi / Hinglish, EN = English. Starred items are recommended starting points. In the app, press
**Follow** on the one you will use (the dashboard then sends you there), or add your own link.


### L1 Git & Developer Tools

- EN * [Complete Git and GitHub Tutorial](https://www.youtube.com/watch?v=apGV9Kg7ics) - Kunal Kushwaha - ~1h13m, covers branching, PRs, conflicts, squashing.
- HI * [Complete Git Tutorials for Beginners in Hindi](https://www.youtube.com/playlist?list=PLu0W_9lII9agwhy658ZPA0MTStKUJTWPi) - CodeWithHarry
- EN [Pro Git book (free)](https://git-scm.com/book/en/v2) - git-scm.com
- HI [Linux Command Line in One Video: 100 commands (Hindi)](https://www.youtube.com/watch?v=Byx4sgLR88E) - MPrashant
- EN [Introduction to Linux - Full Course for Beginners](https://www.youtube.com/watch?v=sWbUDq4S6Y8) - freeCodeCamp.org

### L4 APIs & Web Fundamentals

- EN * [Hussein Nasser - backend fundamentals](https://www.youtube.com/@hnasr) - Hussein Nasser - Best deep explanations of HTTP, TCP, proxies, HTTP/2.
- HI * [Chai aur Backend (Hindi)](https://www.youtube.com/playlist?list=PLu71SKxNbfoBGh_8p_NS-ZAh6v7HhYqHW) - Chai aur Code (Hitesh Choudhary) - Uses Node.js, but the HTTP/API/auth concepts transfer 1:1 to Spring Boot.
- EN [MDN - An overview of HTTP](https://developer.mozilla.org/en-US/docs/Web/HTTP/Overview) - MDN
- EN [springdoc-openapi](https://springdoc.org/) - springdoc
- HI [REST API Best Practices: clean, secure & scalable APIs (Hindi + English)](https://www.youtube.com/watch?v=Nua_rCoeG70)
- EN [Microsoft REST API design best practices](https://learn.microsoft.com/en-us/azure/architecture/best-practices/api-design) - Microsoft Learn

### L5 Databases (SQL & NoSQL)

- HI * [SQL - Complete Course in 3 Hours (MySQL)](https://www.youtube.com/watch?v=hlGoQC332VM) - Apna College
- EN * [Learn PostgreSQL - Full Course for Beginners](https://www.youtube.com/watch?v=qw--VYLpxG4) - freeCodeCamp.org
- EN [Hussein Nasser - database engineering](https://www.youtube.com/@hnasr) - Hussein Nasser - Search his channel for indexes, isolation levels and ACID.
- EN [Use The Index, Luke (free SQL indexing book)](https://use-the-index-luke.com/) - Markus Winand
- EN [PostgreSQL documentation](https://www.postgresql.org/docs/current/) - postgresql.org
- HI [Master MongoDB in One Video - beginner to advanced (Hindi)](https://www.youtube.com/watch?v=tww-gbNPOcA)
- EN [MongoDB University - free courses](https://learn.mongodb.com/) - MongoDB

### L6 Authentication & Security

- HI * [Complete JWT Authentication with Spring Boot 3.1 in one video](https://www.youtube.com/watch?v=q2l91Ffc_8U) - Learn Code With Durgesh
- EN * [Spring Security JWT: secure your Spring Boot REST APIs](https://www.youtube.com/watch?v=KYNR5js2cXE) - Dan Vega - Uses the same OAuth2 Resource Server approach as this playground.
- EN [OWASP Top 10](https://owasp.org/www-project-top-ten/) - OWASP
- EN [Spring Security reference](https://docs.spring.io/spring-security/reference/index.html) - spring.io
- HI [OAuth 2.0 explained (Hindi)](https://www.youtube.com/watch?v=sjvL24fciQg) - MBSA
- HI [Spring Security (Spring Boot Full Course #35) - from your playlist](https://www.youtube.com/watch?v=KVJuG95JSpM) - Coder Army
- HI [OAuth 2.0 & OIDC: Login with Google (Spring Boot Full Course #39)](https://www.youtube.com/watch?v=t9kRN4wMVk0) - Coder Army

### L7 Testing & Clean Code

- HI * [Low Level Design (LLD) - design patterns in Java](https://www.youtube.com/playlist?list=PL6W8uoQQ2c61X_9e6Net0WdYZidm7zooW) - Concept && Coding (Shrayansh Jain)
- EN * [Christopher Okhravi - design patterns](https://www.youtube.com/@ChristopherOkhravi) - Christopher Okhravi
- EN [Testcontainers for Java - getting started](https://testcontainers.com/guides/) - testcontainers.com
- EN [Master Unit Testing in Spring Boot with JUnit 5 & Mockito](https://www.youtube.com/watch?v=zK1O8pIx_oM)
- EN [Spring Boot testing: Zero to Hero (Spring I/O talk)](https://www.youtube.com/watch?v=u5foQULTxHM) - Daniel Garnier-Moiroux
- EN [Spring Boot 3 integration testing with Testcontainers](https://www.youtube.com/watch?v=Q-0Z6KZF1xM) - Java Techie
- HI [Unit Testing in a Spring Boot project in one video (Hindi)](https://www.youtube.com/watch?v=qpK1AoFWY8k)

### L8 Redis & Caching

- HI * [Engineering Digest (Redis with Spring Boot, Hindi)](https://www.youtube.com/@EngineeringDigest) - Engineering Digest
- HI [Piyush Garg (Redis & system design, Hindi)](https://www.youtube.com/@piyushgargdev) - Piyush Garg
- EN * [Redis documentation](https://redis.io/docs/latest/) - redis.io
- EN [Redis Crash Course (2025)](https://www.youtube.com/watch?v=Xw1Lv66noEY)
- HI [Spring Boot API with MySQL & Redis cache (Hindi)](https://www.youtube.com/watch?v=RiWbgJJTC-Q)
- EN [Redis University - free courses](https://university.redis.io/) - Redis

### L9 Kafka & Event-Driven Architecture

- HI * [Master Kafka in a single video (Kafka in Hindi)](https://www.youtube.com/watch?v=ei6fK9StzMM) - Piyush Garg
- EN * [Apache Kafka 101](https://www.youtube.com/playlist?list=PLf38f5LhQtheK16nwnCYFqH23WUUvZfSb) - Confluent (Tim Berglund)
- EN [Spring Boot + Apache Kafka Tutorial](https://www.youtube.com/playlist?list=PLGRDMO4rOGcNLwoack4ZiTyewUcF6y6BU) - Java Guides
- EN [Confluent Developer courses](https://developer.confluent.io/courses/) - Confluent
- HI [Kafka consumer with Spring Boot microservices (Hindi)](https://www.youtube.com/watch?v=r3WW-nMLUSc) - Amit Goyal

### L10 Microservices

- EN * [Java and Spring Boot Microservices - 10 Hour Full Course](https://www.youtube.com/watch?v=1aWhYEynZQw) - Amigoscode
- HI * [High Level Design (microservices patterns, Hindi)](https://www.youtube.com/playlist?list=PL6W8uoQQ2c63W58rpNFDwdrBnq5G3EfT7) - Concept && Coding (Shrayansh Jain)
- EN [microservices.io patterns](https://microservices.io/patterns/) - Chris Richardson
- HI [Microservices using Spring Boot in one video (Hindi)](https://www.youtube.com/watch?v=ubHa5I3yP70)

### L11 System Design

- EN * [System Design playlist](https://www.youtube.com/playlist?list=PLMCXHnjXnTnvo6alSjVkgxV-VH6EPyvoX) - Gaurav Sen
- HI * [High Level Design (HLD) from Basics to Advanced](https://www.youtube.com/playlist?list=PL6W8uoQQ2c63W58rpNFDwdrBnq5G3EfT7) - Concept && Coding (Shrayansh Jain)
- HI [Low Level Design (LLD) from Basics to Advanced](https://www.youtube.com/playlist?list=PL6W8uoQQ2c61X_9e6Net0WdYZidm7zooW) - Concept && Coding (Shrayansh Jain)
- EN [ByteByteGo](https://www.youtube.com/@ByteByteGo) - ByteByteGo
- EN [System Design Primer (GitHub)](https://github.com/donnemartin/system-design-primer) - donnemartin

### L12 Docker & Cloud (AWS)

- EN * [Docker Tutorial for Beginners](https://www.youtube.com/playlist?list=PLy7NrYWoggjzfAHlUusx2wuDwfCrmJYcs) - TechWorld with Nana
- HI * [Docker in One Shot with 2 live DevOps projects](https://www.youtube.com/watch?v=9bSbNNH4Nqw) - TrainWithShubham
- EN [Abhishek Veeramalla - AWS Zero to Hero](https://www.youtube.com/@AbhishekVeeramalla/playlists) - Abhishek Veeramalla - Indian-English explanations, very practical.
- EN [Docker docs - get started](https://docs.docker.com/get-started/) - docker.com
- HI [AWS in One Video for beginners 2026 (Hindi)](https://www.youtube.com/watch?v=N4sJj-SxX00) - MPrashant

### L13 Kubernetes & CI/CD

- EN * [Kubernetes Tutorial for Beginners (4 hours)](https://www.youtube.com/watch?v=X48VuDVv0do) - TechWorld with Nana
- HI * [TrainWithShubham (Kubernetes & DevOps in Hindi)](https://www.youtube.com/@TrainWithShubham) - TrainWithShubham
- EN [GitHub Actions Tutorial - CI/CD pipeline with Docker](https://www.youtube.com/watch?v=R8_veQiYBjI) - TechWorld with Nana
- EN [Abhishek Veeramalla - DevOps Zero to Hero](https://www.youtube.com/@AbhishekVeeramalla/playlists) - Abhishek Veeramalla
- EN [Kubernetes docs - tutorials](https://kubernetes.io/docs/tutorials/) - kubernetes.io
- HI [GitHub Actions full course (Hindi)](https://www.youtube.com/watch?v=je1u_ugTW6Y)

### L14 Observability & Production Engineering

- EN * [How Prometheus Monitoring works](https://www.youtube.com/watch?v=h4Sl21AKiDg) - TechWorld with Nana
- EN [Prometheus Monitoring - full tutorial](https://www.youtube.com/playlist?list=PLy7NrYWoggjxCF3av5JKwyG7FFF9eLeL4) - TechWorld with Nana
- EN [Abhishek Veeramalla - Observability Zero to Hero](https://www.youtube.com/@AbhishekVeeramalla/playlists) - Abhishek Veeramalla
- EN [OpenTelemetry docs - Java](https://opentelemetry.io/docs/languages/java/) - opentelemetry.io
- HI * [Prometheus & Grafana for DevOps - observability (Hindi)](https://www.youtube.com/watch?v=DXZUunEeHqM)

### L15 Real-World Backend Projects & Interview Prep

- HI * [Coder Army (Java, Spring Boot, DSA in Hindi)](https://www.youtube.com/@CoderArmy9) - Coder Army
- EN * [roadmap.sh - backend project ideas](https://roadmap.sh/backend/projects) - roadmap.sh
- HI [Engineering Digest - Spring Boot project playlists (Hindi)](https://www.youtube.com/@EngineeringDigest/playlists) - Engineering Digest

### L16 Bonus: AI Engineering with Spring AI

- EN * [Dan Vega (Spring AI tutorials)](https://www.youtube.com/@DanVega) - Dan Vega
- EN [Spring AI Introduction: Building AI applications in Java](https://www.youtube.com/watch?v=yyvjT0v3lpY)
- EN [Spring AI reference documentation](https://docs.spring.io/spring-ai/reference/) - spring.io
- HI * [Spring AI Full Course - build real AI apps with Spring Boot (Hindi)](https://www.youtube.com/watch?v=KN58GwRRu28)


## DSA

Follow [Striver's A2Z sheet](https://takeuforward.org/dsa/strivers-a2z-sheet-learn-dsa-a-to-z) and its
[video playlist](https://www.youtube.com/playlist?list=PLgUwDviBIf0oF6QL8m22w1hIDC1vJ_BHz) on takeUforward directly.


## What NOT to do (from the roadmap)

- Don't learn 10 languages - master Java deeply.
- Don't jump into Kubernetes before Docker, or Kafka before basic messaging.
- Don't memorise system design diagrams without the "why".
- Don't blindly copy YouTube projects - customise and extend them.
- Don't spend months only watching tutorials. The Rebuild Lab exists so every video ends in typed code.
