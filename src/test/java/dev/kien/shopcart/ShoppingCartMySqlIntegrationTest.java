package dev.kien.shopcart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import dev.kien.shopcart.service.ShoppingCartService;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts = "/db/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM cart_lines",
        "DELETE FROM products",
        "DELETE FROM shopping_carts"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ShoppingCartMySqlIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private ShoppingCartService cartService;

    @Test
    @Sql(statements = {
            "INSERT INTO shopping_carts (id, label) VALUES (10, 'Integration cart')",
            "INSERT INTO products (id, name, description, category, price) "
                    + "VALUES (20, 'Travel mug', 'Insulated stainless steel mug', 'HOME', 180000.00)"
    })
    void addAndReadCartLineAgainstMysql() {
        cartService.addProduct(10L, 20L);
        cartService.addProduct(10L, 20L);

        assertEquals(2, cartService.getLines(10L).getFirst().getQuantity());
        assertEquals(new BigDecimal("360000.00"), cartService.total(10L));
    }
}
