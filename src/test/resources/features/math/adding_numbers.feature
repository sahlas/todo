Feature: Adding Numbers
    Scenario: Add two positive numbers
        Given the first number is 5
        And the second number is 10
        When I add the two numbers
        Then the result should be 15

    Scenario: Add a positive and a negative number
        Given the first number is 7
        And the second number is -3
        When I add the two numbers
        Then the result should be 4

    Scenario: Add two negative numbers
        Given the first number is -4
        And the second number is -6
        When I add the two numbers
        Then the result should be -10