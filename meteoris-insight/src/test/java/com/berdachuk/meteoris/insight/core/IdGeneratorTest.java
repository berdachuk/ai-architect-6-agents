package com.berdachuk.meteoris.insight.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdGeneratorTest {

    @Test
    void generatesValidIdsAndExtractsTime() {
        IdGenerator gen = new IdGenerator();
        String id = gen.generateId();
        assertThat(gen.isValidId(id)).isTrue();
        assertThat(id).hasSize(24);
        Instant t = gen.extractCreationInstant(id);
        assertThat(t).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void generatesUniqueIds() {
        IdGenerator gen = new IdGenerator();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            ids.add(gen.generateId());
        }
        assertThat(ids).hasSize(500);
    }
}
