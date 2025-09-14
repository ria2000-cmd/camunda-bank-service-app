package com.example.bankService.goingHomeProcess;

import com.example.bankService.model.Client;
import com.example.bankService.util.AbstractTestBase;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@DisplayName("Test for going home process (DMN table)")
@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)

public class ChooseTransportToHomeTest extends AbstractTestBase {

    DmnEngine dmnEngine;
    DmnDecision dmnDecision;

    @BeforeEach
    public void setUp(){
        dmnEngine = new DefaultDmnEngineConfiguration().buildEngine();

        var inputStream = getClass().getClassLoader().getResourceAsStream("choose-transport-to-home-dmn.dmn");
        assertNotNull(inputStream, "DMN file is not found");

        var decisionGraph = dmnEngine.parseDecisionRequirementsGraph(inputStream);
        dmnDecision = decisionGraph.getDecision("choose-transport-to-home-dmn");
    }

    public static Stream<Arguments> clientTransportProvider(){
        return Stream.of(
                Arguments.arguments(CLIENT_MONEY_ZERO, "walking"),
                Arguments.arguments(CLIENT_MONEY_9, "walking"),
                Arguments.arguments(CLIENT_MONEY_10, "walking"),
                Arguments.arguments(CLIENT_MONEY__10_1, "cityBus"),
                Arguments.arguments(CLIENT_MONEY_20, "cityBus"),
                Arguments.arguments(CLIENT_MONEY_30, "metro"),
                Arguments.arguments(CLIENT_MONEY_40, "taxi"),
                Arguments.arguments(CLIENT_MONEY_41, "rentCar")
        );
    }

    @DisplayName("testDmnDecision() should return correct 'transport' in different input cases")
    @ParameterizedTest
    @MethodSource("clientTransportProvider")
    void testDmnDecision(Client client, String transport){

        Map<String, Object> variables = new HashMap<>();
        variables.put("client", client);

        var result = dmnEngine.evaluateDecisionTable(dmnDecision, variables);

        assertNotNull(result);
        assertEquals(transport, result.getSingleResult().getEntry("transport"));
    }

}
