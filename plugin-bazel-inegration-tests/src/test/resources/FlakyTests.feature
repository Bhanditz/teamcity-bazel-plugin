Feature: Flaky tests support

    Scenario: BES produce a build problem when flaky_test_attempts were not specified at all
        When use bazel default
        And add the argument test
        And add the target :tests
        And run in FlakyTests
        Then the exit code is 3
        And the stdErr output is empty
        And the result contains all service messages like
        | #            | description  |
        | buildProblem | Build failed |

    Scenario: BES produce a build problem when sufficient flaky_test_attempts were specified
        When use bazel default
        And add the argument test
        And add the argument --flaky_test_attempts
        And add the argument 1
        And add the target :tests
        And run in FlakyTests
        Then the exit code is 3
        And the stdErr output is empty
        And the result contains all service messages like
            | #            | description  |
            | buildProblem | Build failed |

    Scenario Outline: BES does not produce a build problem when sufficient flaky_test_attempts were specified
        When use bazel <version>
        And add the argument test
        And add the argument --flaky_test_attempts
        And add the argument <flaky_test_attempts>
        And add the target :tests
        And run in FlakyTests
        Then the exit code is 0
        And the stdErr output is empty
        And the result does not contain any service messages like
            | #            | description  |
            | buildProblem | Build failed |
        Examples:
            | version | flaky_test_attempts |
            |default   | 2                  |
            |default   | 4                  |