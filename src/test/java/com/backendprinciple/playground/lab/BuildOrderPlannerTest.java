package com.backendprinciple.playground.lab;

import static org.assertj.core.api.Assertions.assertThat;

import com.backendprinciple.playground.lab.BuildOrderPlanner.Candidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildOrderPlannerTest {

    private static Candidate file(String path, String content) {
        return new Candidate(path, FileClassifier.layer(path, content), content);
    }

    @Test
    void ordersLayersLikeASeniorEngineerWouldBuildThem() {
        List<Candidate> ordered = BuildOrderPlanner.order(List.of(
                file("src/main/java/shop/order/OrderController.java", "@RestController class OrderController {}"),
                file("src/test/java/shop/OrderServiceTest.java", "class OrderServiceTest {}"),
                file("src/main/java/shop/order/OrderService.java", "@Service class OrderService {}"),
                file("src/main/java/shop/order/Order.java", "@Entity class Order { OrderStatus status; }"),
                file("src/main/java/shop/order/OrderStatus.java", "public enum OrderStatus { NEW }"),
                file("src/main/java/shop/order/OrderRepository.java", "interface OrderRepository extends JpaRepository<Order, Long> {}"),
                file("src/main/resources/application.yml", "server:\n  port: 8080"),
                file("src/main/java/shop/ShopApplication.java", "@SpringBootApplication class ShopApplication { SpringApplication x; }"),
                file("Dockerfile", "FROM eclipse-temurin:21"),
                file("pom.xml", "<project/>"),
                file("src/main/resources/db/migration/V1__init.sql", "CREATE TABLE orders ();")));

        assertThat(ordered).extracting(c -> FileClassifier.fileName(c.path())).containsExactly(
                "pom.xml", "application.yml", "V1__init.sql", "ShopApplication.java",
                "OrderStatus.java", "Order.java", // enum first: Order uses OrderStatus
                "OrderRepository.java", "OrderService.java", "OrderController.java",
                "OrderServiceTest.java", "Dockerfile");
    }

    @Test
    void cyclesFallBackToAlphabeticalOrder() {
        List<Candidate> ordered = BuildOrderPlanner.order(List.of(
                file("src/main/java/x/model/B.java", "class B { A a; }"),
                file("src/main/java/x/model/A.java", "class A { B b; }")));
        assertThat(ordered).extracting(Candidate::path).containsExactly("src/main/java/x/model/A.java", "src/main/java/x/model/B.java");
    }
}
