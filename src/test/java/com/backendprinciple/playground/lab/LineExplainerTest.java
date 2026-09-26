package com.backendprinciple.playground.lab;

import static org.assertj.core.api.Assertions.assertThat;

import com.backendprinciple.playground.lab.LineExplainer.Explanation;
import com.backendprinciple.playground.lab.LineExplainer.Note;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LineExplainerTest {

    private final LineExplainer explainer = new LineExplainer(JsonMapper.builder().build());

    private static final List<String> SERVICE = """
            package com.example.shop;

            import org.springframework.stereotype.Service;

            @Service
            public class OrderService {

                private final OrderRepository orderRepository;

                public OrderService(OrderRepository orderRepository) {
                    this.orderRepository = orderRepository;
                }

                @Transactional(readOnly = true)
                public OrderResponse get(Long id) {
                    Order order = orderRepository.findById(id).orElseThrow();
                    return OrderResponse.from(order);
                }
            }
            """.lines().toList();

    private Explanation line(int n) {
        return explainer.explain(SERVICE, n, "java");
    }

    private static List<String> terms(Explanation e) {
        return e.notes().stream().map(Note::term).toList();
    }

    @Test
    void explainsPackageImportAndAnnotations() {
        assertThat(line(1).summary()).contains("com/example/shop/");
        assertThat(line(3).notes()).extracting(Note::term).contains("Service");
        assertThat(line(3).notes().getFirst().text()).contains("stereotype");
        assertThat(terms(line(5))).containsExactly("@Service");
    }

    @Test
    void recognisesConstructorInjectionAndFieldAssignment() {
        assertThat(line(10).kind()).isEqualTo("constructor");
        assertThat(line(10).summary()).contains("constructor injection");
        assertThat(line(11).kind()).isEqualTo("assignment");
        assertThat(line(11).context()).isEqualTo("inside the constructor OrderService() of class OrderService");
    }

    @Test
    void explainsMethodBodiesWithContext() {
        Explanation local = line(16);
        assertThat(local.kind()).isEqualTo("variable");
        assertThat(local.context()).isEqualTo("inside get() of class OrderService");
        assertThat(terms(local)).contains("findById", "orElseThrow");
        assertThat(line(14).notes()).extracting(Note::term).containsExactly("@Transactional");
        assertThat(line(15).kind()).isEqualTo("method");
    }

    @Test
    void translatesDerivedQueryNames() {
        Explanation e = explainer.explain(List.of(
                "interface TaskRepository extends JpaRepository<Task, Long> {",
                "    List<Task> findByStatusAndDueDateBeforeOrderByCreatedAtDesc(TaskStatus s, LocalDate d);",
                "}"), 2, "java");
        String sql = e.notes().stream().filter(n -> n.term().equals("derived query")).findFirst().orElseThrow().text();
        assertThat(sql).contains("WHERE status = ? AND due_date < ? ORDER BY created_at DESC");
    }

    @Test
    void rebuildsYamlKeysFromIndentation() {
        List<String> yml = List.of("spring:", "  datasource:", "    url: jdbc:postgresql://localhost/db", "  jpa:",
                "    hibernate:", "      ddl-auto: validate");
        assertThat(LineExplainer.yamlPath(yml, 3)).isEqualTo("spring.datasource.url");
        assertThat(LineExplainer.yamlPath(yml, 6)).isEqualTo("spring.jpa.hibernate.ddl-auto");
        assertThat(explainer.explain(yml, 6, "yaml").notes().getFirst().text()).contains("validate");
    }

    @Test
    void explainsMavenArtifactsSqlAndDocker() {
        assertThat(terms(explainer.explain(List.of("<artifactId>spring-boot-starter-data-jpa</artifactId>"), 1, "xml")))
                .contains("spring-boot-starter-data-jpa");
        assertThat(terms(explainer.explain(List.of("user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,"), 1, "sql")))
                .contains("ON DELETE CASCADE", "NOT NULL", "REFERENCES");
        assertThat(terms(explainer.explain(List.of("USER app"), 1, "dockerfile"))).containsExactly("USER");
    }
}
