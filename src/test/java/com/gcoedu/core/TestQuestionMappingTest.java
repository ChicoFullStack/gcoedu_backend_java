package com.gcoedu.core;

import com.gcoedu.core.domain.entity.tenant.TestQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestQuestionMappingTest {

    @Test
    void mapsToSharedTenantSchemaContract() throws NoSuchFieldException {
        Table table = TestQuestion.class.getAnnotation(Table.class);
        Column orderColumn = TestQuestion.class
                .getDeclaredField("orderIndex")
                .getAnnotation(Column.class);
        Transient weight = TestQuestion.class
                .getDeclaredField("weight")
                .getAnnotation(Transient.class);

        assertThat(table.name()).isEqualTo("test_questions");
        assertThat(table.schema()).isEmpty();
        assertThat(orderColumn.name()).isEqualTo("\"order\"");
        assertThat(weight).isNotNull();
    }
}
