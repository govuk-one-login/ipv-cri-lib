package uk.gov.di.ipv.cri.common.library.util;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventProbeTest {

    @Mock private Metrics mockMetrics;
    @Captor private ArgumentCaptor<DimensionSet> dimensionSetCaptor;

    private EventProbe eventProbe;

    @BeforeEach
    void setUp() {
        eventProbe = new EventProbe(mockMetrics);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void createsEventProbeWithDefaultConstructor() {
        eventProbe = new EventProbe();

        assertNotNull(eventProbe);
    }

    @Test
    void createsEventProbeWithMetricsParameter() {
        assertNotNull(eventProbe);
    }

    @Test
    void logsThrowableWithErrorLevel() {
        RuntimeException exception = new RuntimeException("Test exception");

        EventProbe result = eventProbe.log(Level.ERROR, exception);

        assertSame(eventProbe, result);
    }

    @Test
    void logsThrowableWithInfoLevel() {
        RuntimeException exception = new RuntimeException("Test exception");

        EventProbe result = eventProbe.log(Level.INFO, exception);

        assertSame(eventProbe, result);
    }

    @Test
    void logsThrowableWithNestedCause() {
        RuntimeException rootCause = new RuntimeException("Root cause");
        RuntimeException middleCause = new RuntimeException("Middle cause", rootCause);
        RuntimeException exception = new RuntimeException("Top exception", middleCause);

        EventProbe result = eventProbe.log(Level.ERROR, exception);

        assertSame(eventProbe, result);
    }

    @Test
    void logsMessage() {
        EventProbe result = eventProbe.log(Level.INFO, "Test message");

        assertSame(eventProbe, result);
    }

    @Test
    void addsCounterMetricWithDefaultValue() {
        EventProbe result = eventProbe.counterMetric("test-metric");

        verify(mockMetrics).addMetric("test-metric", 1.0);
        assertSame(eventProbe, result);
    }

    @Test
    void addsCounterMetricWithValueAndUnit() {
        EventProbe result = eventProbe.counterMetric("test-metric", 10.0, MetricUnit.MILLISECONDS);

        verify(mockMetrics).addMetric("test-metric", 10.0, MetricUnit.MILLISECONDS);
        assertSame(eventProbe, result);
    }

    @Test
    void logsAuditEvent() {
        Object testEvent = new Object();

        EventProbe result = eventProbe.auditEvent(testEvent);

        assertSame(eventProbe, result);
    }

    @Test
    void addsFieldToLoggingContext() {
        EventProbe result = eventProbe.addFieldToLoggingContext("testKey", "testValue");

        assertEquals("testValue", MDC.get("testKey"));
        assertSame(eventProbe, result);
    }

    @Test
    void addsJourneyIdToLoggingContextWhenNotBlank() {
        String journeyId = "test-journey-id";

        EventProbe result = eventProbe.addJourneyIdToLoggingContext(journeyId);

        assertEquals(journeyId, MDC.get("govuk_signin_journey_id"));
        assertSame(eventProbe, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @NullSource
    void doesNotAddJourneyIdToLoggingContextWhenBlankOrNull(String journeyId) {
        EventProbe result = eventProbe.addJourneyIdToLoggingContext(journeyId);

        assertNull(MDC.get("govuk_signin_journey_id"));
        assertSame(eventProbe, result);
    }

    @Test
    void addDimensions() {
        eventProbe.addDimensions(Map.of("Name", "Value"));

        verify(mockMetrics).addDimension(dimensionSetCaptor.capture());
        DimensionSet capturedValue = dimensionSetCaptor.getValue();

        assertFalse(capturedValue.getDimensionKeys().isEmpty());
        assertThat(capturedValue.getDimensionKeys().iterator().next(), equalTo("Name"));
        assertThat(capturedValue.getDimensionValue("Name"), equalTo("Value"));
    }

    @Test
    void addsMultipleDimensions() {
        Map<String, String> dimensions =
                Map.of(
                        "dimension1", "value1",
                        "dimension2", "value2");

        eventProbe.addDimensions(dimensions);

        verify(mockMetrics).addDimension(dimensionSetCaptor.capture());
        DimensionSet capturedValue = dimensionSetCaptor.getValue();

        assertEquals(2, capturedValue.getDimensionKeys().size());
        assertTrue(capturedValue.getDimensionKeys().contains("dimension1"));
        assertTrue(capturedValue.getDimensionKeys().contains("dimension2"));
        assertEquals("value1", capturedValue.getDimensionValue("dimension1"));
        assertEquals("value2", capturedValue.getDimensionValue("dimension2"));
    }

    @Test
    void doesNotAddDimensionsWhenNull() {
        eventProbe.addDimensions(null);

        verify(mockMetrics, never()).addDimension(any());
    }

    @Test
    void addsEmptyDimensions() {
        eventProbe.addDimensions(Map.of());

        verify(mockMetrics).addDimension(dimensionSetCaptor.capture());
        DimensionSet capturedValue = dimensionSetCaptor.getValue();

        assertTrue(capturedValue.getDimensionKeys().isEmpty());
    }
}
