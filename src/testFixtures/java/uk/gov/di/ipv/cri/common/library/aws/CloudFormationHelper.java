package uk.gov.di.ipv.cri.common.library.aws;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;

import java.security.InvalidParameterException;
import java.util.List;

/**
 * A utility class to retrieve stack and it's outputs and parameters. The class is a wrapper around
 * {@link CloudFormationClient} and allows to get around retrieving the mock SQS url used for tests
 */
public final class CloudFormationHelper {

    /**
     * Client for accessing AWS CloudFormation. All service calls made using this client are
     * blocking, and will not return until the service call completes.
     */
    private static final CloudFormationClient cloudFormation = CloudFormationClient.create();

    private CloudFormationHelper() {}

    /**
     * Returns the parameter data type from the stack. ParameterKey - The key associated with the
     * parameter. If you don't specify a key and value for a particular parameter, AWS
     * CloudFormation uses the default value that's specified in your template. ParameterValue - The
     * input value associated with the parameter.
     */
    public static String getParameter(String stackName, String parameterName) {
        return getStack(stackName).parameters().stream()
                .filter(parameter -> parameter.parameterKey().equals(parameterName))
                .findFirst()
                .orElseThrow(
                        () ->
                                new InvalidParameterException(
                                        String.format(
                                                "Could not get parameter %s from stack %s",
                                                parameterName, stackName)))
                .parameterValue();
    }

    /**
     * Returns the output data type from the stack. OutputKey - The key associated with the output.
     * OutputValue - The value associated with the output.
     */
    public static String getOutput(String stackName, String outputName) {
        Stack stack = getStack(stackName);

        return stack.outputs().stream()
                .filter(output -> output.outputKey().equals(outputName))
                .findFirst()
                .orElseThrow(
                        () ->
                                new InvalidParameterException(
                                        String.format(
                                                "Could not get output %s from stack %s",
                                                outputName, stackName)))
                .outputValue();
    }

    /**
     * Returns the description for the specified stack; if no stack name was specified, then it
     * returns the description for all the stacks created.
     */
    private static Stack getStack(String stackName) {
        DescribeStacksRequest request =
                DescribeStacksRequest.builder().stackName(stackName).build();

        DescribeStacksResponse response = cloudFormation.describeStacks(request);

        List<Stack> stacks = response.stacks();

        if (stacks.isEmpty()) {
            throw new IllegalArgumentException("Stack not found: " + stackName);
        }

        return stacks.get(0);
    }
}
