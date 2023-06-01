package com.flightstats.hub.webhook;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebhookLeaderStateRunningStateTest {
    @ParameterizedTest
    @MethodSource("buildTestData")
    void testRunning(TestData testData) {
        assertEquals(testData.isRunning(), testData.getState().isRunningOnSingleServer());
    }

    @ParameterizedTest
    @MethodSource("buildTestData")
    void testStopped(TestData testData) {
        assertEquals(testData.isStopped(), testData.getState().isStopped());
    }

    @ParameterizedTest
    @MethodSource("buildTestData")
    void testAbnormalState(TestData testData) {
        assertEquals(testData.isInAbnormalState(), testData.getState().isRunningInAbnormalState());
    }

    private static Stream<TestData> buildTestData() {
        WebhookLeaderState.RunningState state = WebhookLeaderState.RunningState.builder().build();
        return Stream.of(
                TestData.builder().testName("stopped")
                        .state(state
                                .withLeadershipAcquired(false)
                                .withRunningServers(newHashSet()))
                        .running(false)
                        .stopped(true)
                        .inAbnormalState(false)
                        .build(),

                TestData.builder().testName("running on one server")
                        .state(state
                                .withLeadershipAcquired(true)
                                .withRunningServers(newHashSet("a")))
                        .running(true)
                        .stopped(false)
                        .inAbnormalState(false)
                        .build(),

                TestData.builder().testName("running on more than one server")
                        .state(state
                                .withLeadershipAcquired(true)
                                .withRunningServers(newHashSet("a", "b")))
                        .running(false)
                        .stopped(false)
                        .inAbnormalState(true)
                        .build(),

                TestData.builder().testName("reports as running on one server, but the server hasn't set a lock")
                        .state(state
                                .withLeadershipAcquired(false)
                                .withRunningServers(newHashSet("a")))
                        .running(false)
                        .stopped(false)
                        .inAbnormalState(true)
                        .build(),

                TestData.builder().testName("reports as running on multiple servers, but the server hasn't set a lock")
                        .state(state
                                .withLeadershipAcquired(false)
                                .withRunningServers(newHashSet("a", "b")))
                        .running(false)
                        .stopped(false)
                        .inAbnormalState(true)
                        .build(),

                TestData.builder().testName("has a lock that says it's running, but we don't know where")
                        .state(state
                                .withLeadershipAcquired(true)
                                .withRunningServers(newHashSet()))
                        .running(false)
                        .stopped(false)
                        .inAbnormalState(true)
                        .build()
        );
    }

    @Builder
    @Wither
    @Value
    private static class TestData {
        String testName;
        WebhookLeaderState.RunningState state;

        boolean running;
        boolean stopped;
        boolean inAbnormalState;
    }
}
