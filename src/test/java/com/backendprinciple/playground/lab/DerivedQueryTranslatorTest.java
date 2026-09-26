package com.backendprinciple.playground.lab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DerivedQueryTranslatorTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            find   | Email                               | SELECT * FROM <table> WHERE email = ?
            find   | TitleContainingIgnoreCase           | WHERE lower(title) LIKE '%' || lower(?) || '%'
            count  | StatusAndPriority                   | SELECT COUNT(*) FROM <table> WHERE status = ? AND priority = ?
            exists | EmailOrUsername                     | WHERE email = ? OR username = ?
            find   | PriceBetweenOrderByNameAsc          | WHERE price BETWEEN ? AND ? ORDER BY name
            find   | DeletedAtIsNull                     | WHERE deleted_at IS NULL
            """)
    void translates(String verb, String criteria, String expected) {
        assertThat(DerivedQueryTranslator.describe(verb, criteria)).contains(expected);
    }
}
