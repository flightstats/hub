package com.flightstats.hub.webhook;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.newHashSet;
import static junit.framework.Assert.assertEquals;

public class WebhookCoordinatorActionDirectorTest {
    @ParameterizedTest
    @MethodSource("buildTestData")
    void testShouldStart(TestData testData) {
        WebhookCoordinator.WebhookActionDirector director = testData.getDirector();

        assertEquals(testData.getTestName(), testData.isExpectedToStart(), director.webhookShouldStart());
    }

    @ParameterizedTest
    @MethodSource("buildTestData")
    void testShouldStop(TestData testData) {
        WebhookCoordinator.WebhookActionDirector director = testData.getDirector();

        assertEquals(testData.getTestName(), testData.isExpectedToStop(), director.webhookShouldStop());
    }

    @ParameterizedTest
    @MethodSource("buildTestData")
    void testShouldDoNothing(TestData testData) {
        WebhookCoordinator.WebhookActionDirector director = testData.getDirector();

        assertEquals(testData.getTestName(), testData.isExpectedToDoNothing(), director.webhookRequiresNoChanges());
    }

    @ParameterizedTest
    @MethodSource("buildTestData")
    void testShouldRestartOnJustOneServer(TestData testData) {
        WebhookCoordinator.WebhookActionDirector director = testData.getDirector();

        assertEquals(testData.getTestName(), testData.isExpectedToRestart(), director.webhookShouldRestartOnOneServerAndStopAnyOthers());
    }

    private static Stream<TestData> buildTestData() {
        return Stream.of(
                TestData.builder().testName("not started or paused should start")
                        .webhookPaused(false)
                        .runningServers(Collections.emptySet())
                        .changed(false)
                        .expectedToStart(true)
                        .build(),
                TestData.builder().testName("not started or paused but changed should start")
                        .webhookPaused(false)
                        .runningServers(Collections.emptySet())
                        .changed(true)
                        // this is a weird state that shouldn't happen, but both actions are acceptable
                        .expectedToStart(true)
                        .expectedToRestart(true)
                        .build(),
                TestData.builder().testName("not started and paused should do nothing")
                        .webhookPaused(true)
                        .runningServers(Collections.emptySet())
                        .changed(false)
                        .expectedToDoNothing(true)
                        .build(),
                TestData.builder().testName("not started and paused, but updated, should do nothing")
                        .webhookPaused(true)
                        .runningServers(Collections.emptySet())
                        .changed(true)
                        .expectedToDoNothing(true)
                        .build(),
                TestData.builder().testName("started and updated should restart")
                        .webhookPaused(false)
                        .runningServers(newHashSet("server1"))
                        .changed(true)
                        .expectedToRestart(true)
                        .build(),
                TestData.builder().testName("started on one server and not paused should do nothing")
                        .webhookPaused(false)
                        .runningServers(newHashSet("server1"))
                        .changed(false)
                        .expectedToDoNothing(true)
                        .build(),
                TestData.builder().testName("started on two servers and not paused should restart")
                        .webhookPaused(false)
                        .runningServers(newHashSet("server1", "server2"))
                        .changed(false)
                        .expectedToRestart(true)
                        .build(),
                TestData.builder().testName("started on two servers and not paused but updated should should restart")
                        .webhookPaused(false)
                        .runningServers(newHashSet("server1", "server2"))
                        .changed(true)
                        .expectedToRestart(true)
                        .build(),
                TestData.builder().testName("started on two servers and paused should stop")
                        .webhookPaused(true)
                        .runningServers(newHashSet("server1", "server2"))
                        .changed(false)
                        .expectedToStop(true)
                        .build()
        );
    }

    @Builder
    @Wither
    @Value
    private static class TestData {
        String testName;
        boolean webhookPaused;
        Set<String> runningServers;
        boolean changed;

        @Builder.Default
        boolean expectedToStop = false;
        @Builder.Default
        boolean expectedToStart = false;
        @Builder.Default
        boolean expectedToDoNothing = false;
        @Builder.Default
        boolean expectedToRestart = false;

        WebhookCoordinator.WebhookActionDirector getDirector() {
            Webhook webhook = Webhook.builder().paused(isWebhookPaused()).build();

            WebhookLeaderState.RunningState state = new WebhookLeaderState.RunningState(
                    getLeadershipAcquired(),
                    getRunningServers());
            return new WebhookCoordinator.WebhookActionDirector(webhook, state, changed);
        }

        boolean getLeadershipAcquired() {
            return !runningServers.isEmpty();
        }

    }

    @Builder
    @Wither
    @Value
    private static class Expectation {
    }


}
