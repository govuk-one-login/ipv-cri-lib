package uk.gov.di.ipv.cri.common.library.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.MetricsUtils;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventProbeTest {

    @Mock private MetricsLogger mockMetricsLogger;

    @Captor private ArgumentCaptor<DimensionSet> sessionItemArgumentCaptor;

    @Test
    void shouldAddDimensions() {
        try (MockedStatic<MetricsUtils> metricsUtilsMockedStatic =
                Mockito.mockStatic(MetricsUtils.class)) {
            metricsUtilsMockedStatic
                    .when(MetricsUtils::metricsLogger)
                    .thenReturn(mockMetricsLogger);

            EventProbe eventProbe = new EventProbe();
            eventProbe.addDimensions(Map.of("Name", "Value"));

            verify(mockMetricsLogger).putDimensions(sessionItemArgumentCaptor.capture());
            DimensionSet capturedValue = sessionItemArgumentCaptor.getValue();
            assertFalse(capturedValue.getDimensionKeys().isEmpty());
            assertThat(capturedValue.getDimensionKeys().iterator().next(), equalTo("Name"));
            assertThat(capturedValue.getDimensionValue("Name"), equalTo("Value"));
        }
    }
}
