# Your Learning Path

> learn -> build -> break -> debug -> improve -> repeat

This guide turns the *Backend Engineering Roadmap 2026* into a routine. The same data (with progress tracking)
lives in the app under **Roadmap** and **Planner**. Admins can edit resources in the app without redeploying.

## Your two main playlists

| Language | Playlist | Use it for |
|---|---|---|
| Java | [Java Full Course 2026 - Coder Army](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p) (Hindi) | Levels 0 and 2 |
| Spring Boot | [Spring Boot Full Course 2026](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (Hindi) | Level 3 onwards |

> The Spring Boot link you gave has a much shorter id than usual YouTube playlist links. If it doesn't open,
> copy the full link from the address bar and update it under **Admin -> Resources** (Level 3).

### How to combine a video with the Rebuild Lab

1. **Watch** one video at 1x (no coding yet), taking 3-5 bullet notes in the topic's notes box.
2. **Rebuild**: open the matching file in the Rebuild Lab (e.g. an entity video -> the `DOMAIN` step) and
   type it in normal mode. Click any line you don't understand; ask the AI mentor if the offline
   explanation isn't enough.
3. **Recall**: retype the same file in **Blind mode**. Wherever you get stuck is what to rewatch.
4. **Break it**: change something in your own project (remove `@Transactional`, rename a repository
   method, drop an index) and predict what happens before you run it.
5. **Log** the time on the dashboard and mark the topic done in the Roadmap.

Suggested order in the Lab: **Task Manager API** template (a weekend) -> **your own project** -> the
**Backend Playground** template (advanced: JWT, rate limiting, caching, Spring AI).

## 6-month plan (about 20 hours per week)

| Month | Focus | Goal |
|---|---|---|
| 1 | Java + DSA + Git + Linux (L0-L2) | Master the syntax and the CLI, solve basic algorithms |
| 2 | Spring Boot + REST + PostgreSQL (L3-L5) | Build your first CRUD APIs on a real database; rebuild your project |
| 3 | Security + Testing (L6-L7) | Add JWT, write JUnit/Mockito/Testcontainers tests |
| 4 | Redis + Kafka + Microservices (L8-L10) | Caching, events, service boundaries |
| 5 | Docker + AWS + Kubernetes + CI/CD + Observability (L12-L14) | Containerise, deploy, monitor |
| 6 | System Design + Projects + Interview prep (L11, L15, bonus Spring AI) | Capstone food-delivery backend, mock interviews |

A good week: 5 weekdays x 2 h (video + Lab) + 1 weekend day x 8 h (project work) + 2 h revision.

## Resources by level

HI = Hindi / Hinglish, EN = English. Starred items are the recommended starting point.
"Search" links are YouTube searches, used where no single video is clearly best.

### L0 Programming Fundamentals and L2 Core Java
- HI * [Java Full Course 2026 - Coder Army](https://www.youtube.com/playlist?list=PLQEaRBV9gAFsR15tNo2QLF9d2qc-c018p)
- HI [Java Basics to Advanced - Concept && Coding (Shrayansh Jain)](https://www.youtube.com/playlist?list=PL6W8uoQQ2c63f469AyV78np0rbxRFppkx) - JVM memory, collections internals, multithreading
- EN [Striver's A2Z DSA Course - take U forward](https://www.youtube.com/playlist?list=PLgUwDviBIf0oF6QL8m22w1hIDC1vJ_BHz) - Big O, sorting, searching
- EN [Telusko](https://www.youtube.com/@Telusko) - short concept videos
- EN [dev.java learning path](https://dev.java/learn/)

### L1 Git & Developer Tools
- EN * [Complete Git and GitHub Tutorial - Kunal Kushwaha](https://www.youtube.com/watch?v=apGV9Kg7ics)
- HI * [Complete Git Tutorials for Beginners in Hindi - CodeWithHarry](https://www.youtube.com/playlist?list=PLu0W_9lII9agwhy658ZPA0MTStKUJTWPi)
- HI/EN Linux command line: [Hindi search](https://www.youtube.com/results?search_query=linux+commands+for+beginners+hindi) / [English search](https://www.youtube.com/results?search_query=linux+command+line+full+course+beginners)

### L3 Spring Boot
- HI * [Spring Boot Full Course 2026](https://www.youtube.com/playlist?list=PLEYgx5hMdopw) (your playlist)
- HI [Spring Boot from Basics to Advanced - Concept && Coding](https://www.youtube.com/playlist?list=PL6W8uoQQ2c60g6_fcjDCLHSx1LBeVYqyZ)
- HI [Engineering Digest](https://www.youtube.com/@EngineeringDigest)
- EN * [Spring Boot Tutorial - Full Course - Amigoscode](https://www.youtube.com/watch?v=9SGDpanrc8U)
- EN [Spring Boot reference docs](https://docs.spring.io/spring-boot/index.html), [Spring Guides](https://spring.io/guides)

### L4 APIs & Web Fundamentals
- EN * [Hussein Nasser](https://www.youtube.com/@hnasr) - HTTP, TCP, HTTP/2, proxies
- HI [Chai aur Backend - Hitesh Choudhary](https://www.youtube.com/playlist?list=PLu71SKxNbfoBGh_8p_NS-ZAh6v7HhYqHW) - Node.js, but the HTTP/API concepts carry over directly
- HI [REST API design (search)](https://www.youtube.com/results?search_query=rest+api+design+best+practices+hindi)
- EN [MDN - HTTP overview](https://developer.mozilla.org/en-US/docs/Web/HTTP/Overview), [springdoc-openapi](https://springdoc.org/)

### L5 Databases
- HI * [SQL Complete Course in 3 Hours - Apna College](https://www.youtube.com/watch?v=hlGoQC332VM)
- EN * [Learn PostgreSQL - Full Course - freeCodeCamp](https://www.youtube.com/watch?v=qw--VYLpxG4)
- EN [Use The Index, Luke](https://use-the-index-luke.com/) - indexing, free book
- HI [MongoDB (search)](https://www.youtube.com/results?search_query=mongodb+tutorial+hindi)

### L6 Authentication & Security
- HI * [Complete JWT Authentication with Spring Boot 3.1 - Learn Code With Durgesh](https://www.youtube.com/watch?v=q2l91Ffc_8U)
- EN * [Spring Security JWT - Dan Vega](https://www.youtube.com/watch?v=KYNR5js2cXE) - the same OAuth2 Resource Server approach this playground uses
- HI [OAuth 2.0 explained (search)](https://www.youtube.com/results?search_query=oauth+2.0+explained+hindi)
- EN [OWASP Top 10](https://owasp.org/www-project-top-ten/)

### L7 Testing & Clean Code
- HI * [Low Level Design (design patterns in Java) - Concept && Coding](https://www.youtube.com/playlist?list=PL6W8uoQQ2c61X_9e6Net0WdYZidm7zooW)
- EN * [Spring Boot testing: JUnit 5, Mockito, Testcontainers (search)](https://www.youtube.com/results?search_query=spring+boot+testing+junit+5+mockito+testcontainers)
- HI [Spring Boot JUnit Mockito (search)](https://www.youtube.com/results?search_query=spring+boot+junit+mockito+hindi)
- EN [Christopher Okhravi - design patterns](https://www.youtube.com/@ChristopherOkhravi), [Testcontainers guides](https://testcontainers.com/guides/)

### L8 Redis & Caching
- HI * [Engineering Digest](https://www.youtube.com/@EngineeringDigest) - Redis with Spring Boot
- HI [Piyush Garg](https://www.youtube.com/@piyushgargdev)
- EN * [Redis crash course (search)](https://www.youtube.com/results?search_query=redis+crash+course)
- EN [Redis docs](https://redis.io/docs/latest/)

### L9 Kafka & Event-Driven Architecture
- HI * [Master Kafka in a single video - Piyush Garg](https://www.youtube.com/watch?v=ei6fK9StzMM)
- EN * [Apache Kafka 101 - Confluent (Tim Berglund)](https://www.youtube.com/playlist?list=PLf38f5LhQtheK16nwnCYFqH23WUUvZfSb)
- EN [Spring Boot + Apache Kafka Tutorial - Java Guides](https://www.youtube.com/playlist?list=PLGRDMO4rOGcNLwoack4ZiTyewUcF6y6BU)
- HI [Spring Boot Kafka (search)](https://www.youtube.com/results?search_query=spring+boot+kafka+hindi)

### L10 Microservices
- EN * [Java and Spring Boot Microservices - 10 hour course - Amigoscode](https://www.youtube.com/watch?v=1aWhYEynZQw)
- HI * [High Level Design - Concept && Coding](https://www.youtube.com/playlist?list=PL6W8uoQQ2c63W58rpNFDwdrBnq5G3EfT7)
- HI [Spring Boot microservices (search)](https://www.youtube.com/results?search_query=spring+boot+microservices+hindi+full+course)
- EN [microservices.io patterns](https://microservices.io/patterns/)

### L11 System Design
- EN * [System Design playlist - Gaurav Sen](https://www.youtube.com/playlist?list=PLMCXHnjXnTnvo6alSjVkgxV-VH6EPyvoX)
- HI * [HLD from Basics to Advanced - Concept && Coding](https://www.youtube.com/playlist?list=PL6W8uoQQ2c63W58rpNFDwdrBnq5G3EfT7) and [LLD](https://www.youtube.com/playlist?list=PL6W8uoQQ2c61X_9e6Net0WdYZidm7zooW)
- EN [ByteByteGo](https://www.youtube.com/@ByteByteGo), [System Design Primer](https://github.com/donnemartin/system-design-primer)

### L12 Docker & Cloud
- EN * [Docker Tutorial for Beginners - TechWorld with Nana](https://www.youtube.com/playlist?list=PLy7NrYWoggjzfAHlUusx2wuDwfCrmJYcs)
- HI * [Docker in One Shot - TrainWithShubham](https://www.youtube.com/watch?v=9bSbNNH4Nqw)
- EN [Abhishek Veeramalla - AWS/DevOps Zero to Hero](https://www.youtube.com/@AbhishekVeeramalla/playlists)
- HI [AWS for beginners (search)](https://www.youtube.com/results?search_query=aws+tutorial+for+beginners+hindi)

### L13 Kubernetes & CI/CD
- EN * [Kubernetes Tutorial for Beginners (4 h) - TechWorld with Nana](https://www.youtube.com/watch?v=X48VuDVv0do)
- HI * [TrainWithShubham](https://www.youtube.com/@TrainWithShubham)
- EN [GitHub Actions CI/CD with Docker - TechWorld with Nana](https://www.youtube.com/watch?v=R8_veQiYBjI)
- HI [GitHub Actions (search)](https://www.youtube.com/results?search_query=github+actions+ci+cd+hindi)

### L14 Observability
- EN * [How Prometheus Monitoring works - TechWorld with Nana](https://www.youtube.com/watch?v=h4Sl21AKiDg) and the [full Prometheus playlist](https://www.youtube.com/playlist?list=PLy7NrYWoggjxCF3av5JKwyG7FFF9eLeL4)
- EN [Abhishek Veeramalla - Observability Zero to Hero](https://www.youtube.com/@AbhishekVeeramalla/playlists)
- HI * [Prometheus + Grafana (search)](https://www.youtube.com/results?search_query=prometheus+grafana+tutorial+hindi)
- EN [OpenTelemetry for Java](https://opentelemetry.io/docs/languages/java/)

### L15 Projects & Interview Prep
- HI * [Coder Army](https://www.youtube.com/@CoderArmy9) - Java, Spring Boot, DSA
- EN * [Striver's A2Z DSA Course](https://www.youtube.com/playlist?list=PLgUwDviBIf0oF6QL8m22w1hIDC1vJ_BHz) + [A2Z sheet](https://takeuforward.org/dsa/strivers-a2z-sheet-learn-dsa-a-to-z)
- EN [roadmap.sh backend project ideas](https://roadmap.sh/backend/projects)

### Bonus: Spring AI
- EN * [Dan Vega](https://www.youtube.com/@DanVega) - Spring AI tutorials
- EN [Spring AI Introduction: Building AI applications in Java](https://www.youtube.com/watch?v=yyvjT0v3lpY)
- HI * [Spring AI (search)](https://www.youtube.com/results?search_query=spring+ai+tutorial+hindi)
- EN [Spring AI reference](https://docs.spring.io/spring-ai/reference/)

## What NOT to do (from the roadmap)

- Don't learn 10 languages - master Java deeply.
- Don't jump into Kubernetes before Docker, or Kafka before basic messaging.
- Don't memorise system design diagrams without the "why".
- Don't blindly copy YouTube projects - customise and extend them.
- Don't spend months only watching tutorials. The Rebuild Lab exists so every video ends in typed code.
