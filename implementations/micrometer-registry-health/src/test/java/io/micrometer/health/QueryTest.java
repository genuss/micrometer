/**
 * Copyright 2020 VMware, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.health;

import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.util.TimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.micrometer.core.instrument.MockClock.clock;
import static org.assertj.core.api.Assertions.assertThat;

class QueryTest {
    HealthMeterRegistry registry = HealthMeterRegistry
            .builder(HealthConfig.DEFAULT)
            // just so my.timer doesn't get filtered out eagerly
            .serviceLevelObjectives(ServiceLevelObjective.build("timer")
                .count(s -> s.name("my.timer")).isGreaterThan(0))
            .clock(new MockClock())
            .build();

    @BeforeEach
    void before() {
        registry.timer("my.timer", "k", "v1").record(1, TimeUnit.SECONDS);

        Timer t2 = registry.timer("my.timer", "k", "v2");
        t2.record(2, TimeUnit.SECONDS);
        t2.record(2, TimeUnit.SECONDS);

        clock(registry).addSeconds(10);
        registry.tick();
    }

    @Test
    void count() {
        assertThat(
                ServiceLevelObjective
                        .build("timer.throughput")
                        .count(s -> s.name("my.timer"))
                        .getValue(registry)
        ).isEqualTo(3);
    }

    @Test
    void max() {
        assertThat(
                TimeUtils.nanosToUnit(
                        ServiceLevelObjective
                                .build("timer.max")
                                .max(s -> s.name("my.timer"))
                                .getValue(registry),
                        TimeUnit.SECONDS
                )
        ).isEqualTo(2);
    }

    @Test
    void total() {
        assertThat(
                TimeUtils.nanosToUnit(
                        ServiceLevelObjective
                                .build("timer.total")
                                .total(s -> s.name("my.timer"))
                                .getValue(registry),
                        TimeUnit.SECONDS
                )
        ).isEqualTo(5);
    }

    @Test
    void divideBy() {
        assertThat(
                ServiceLevelObjective
                        .build("quotient")
                        .count(s -> s.name("my.timer").tag("k", "v1"))
                        .dividedBy(denom -> denom.count(s -> s.name("my.timer").tag("k", "v2")))
                        .getValue(registry)
        ).isEqualTo(1.0 / 2);
    }

    @Test
    void plus() {
        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .count(s -> s.name("my.timer").tag("k", "v1"))
                        .plus(with -> with.count(s -> s.name("my.timer").tag("k", "v2")))
                        .getValue(registry)
        ).isEqualTo(1 + 2);
    }

    @Test
    void minus() {
        assertThat(
                ServiceLevelObjective
                        .build("difference")
                        .count(s -> s.name("my.timer").tag("k", "v1"))
                        .minus(with -> with.count(s -> s.name("my.timer").tag("k", "v2")))
                        .getValue(registry)
        ).isEqualTo(1 - 2);
    }

    @Test
    void multipliedBy() {
        assertThat(
                ServiceLevelObjective
                        .build("product")
                        .count(s -> s.name("my.timer").tag("k", "v1"))
                        .multipliedBy(by -> by.count(s -> s.name("my.timer").tag("k", "v2")))
                        .getValue(registry)
        ).isEqualTo(1.0 * 2);
    }

    @Test
    void isLessThan() {
        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .count(s -> s.name("my.timer"))
                        .isLessThan(4)
                        .healthy(registry)
        ).isEqualTo(true);

        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .total(s -> s.name("my.timer"))
                        .isLessThan(Duration.ofSeconds(6))
                        .healthy(registry)
        ).isEqualTo(true);
    }

    @Test
    void isLessThanOrEqualTo() {
        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .count(s -> s.name("my.timer"))
                        .isLessThanOrEqualTo(3)
                        .healthy(registry)
        ).isEqualTo(true);

        assertThat(ServiceLevelObjective
                .build("sum")
                .total(s -> s.name("my.timer"))
                .isLessThanOrEqualTo(Duration.ofSeconds(5))
                .healthy(registry)
        ).isEqualTo(true);
    }

    @Test
    void isGreaterThan() {
        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .count(s -> s.name("my.timer"))
                        .isGreaterThan(2)
                        .healthy(registry)
        ).isEqualTo(true);

        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .total(s -> s.name("my.timer"))
                        .isGreaterThan(Duration.ofSeconds(4))
                        .healthy(registry)
        ).isEqualTo(true);
    }

    @Test
    void isGreaterThanOrEqualTo() {
        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .count(s -> s.name("my.timer"))
                        .isGreaterThanOrEqualTo(3)
                        .healthy(registry)
        ).isEqualTo(true);

        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .total(s -> s.name("my.timer"))
                        .isGreaterThanOrEqualTo(Duration.ofSeconds(5))
                        .healthy(registry)
        ).isEqualTo(true);
    }

    @Test
    void isEqualTo() {
        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .count(s -> s.name("my.timer"))
                        .isEqualTo(3)
                        .healthy(registry)
        ).isEqualTo(true);

        assertThat(
                ServiceLevelObjective
                        .build("sum")
                        .total(s -> s.name("my.timer"))
                        .isEqualTo(Duration.ofSeconds(5))
                        .healthy(registry)
        ).isEqualTo(true);
    }
}
