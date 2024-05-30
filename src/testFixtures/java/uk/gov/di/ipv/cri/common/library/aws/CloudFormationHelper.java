package uk.gov.di.ipv.cri.common.library.aws;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;

/**
 * A wrapper class around {@link CloudFormationClient} to retrieve information about CloudFormation
 * stacks
 */
public final class CloudFormationHelper {

    private static final CloudFormationClient CLOUD_FORMATION = CloudFormationClient.create();

    private CloudFormationHelper() {}

    /** Get a parameter value from a stack */
    public static String getParameter(String stackName, String parameterName) {
        return getStack(stackName).parameters().stream()
                .filter(parameter -> parameter.parameterKey().equals(parameterName))
                .map(Parameter::parameterValue)
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format(
                                                "Could not get parameter %s from stack %s",
                                                parameterName, stackName)));
    }

    /** Get an output value from a stack */
    public static String getOutput(String stackName, String outputName) {
        return getStack(stackName).outputs().stream()
                .filter(output -> output.outputKey().equals(outputName))
                .map(Output::outputValue)
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format(
                                                "Could not get output %s from stack %s",
                                                outputName, stackName)));
    }

    private static Stack getStack(String stackName) {
        return CLOUD_FORMATION
                .describeStacks(request -> request.stackName(stackName))
                .stacks()
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format("Could not find stack %s", stackName)));
    }
}
