package com.sahlas.steps;

import io.cucumber.java.en.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.sahlas.app.Calculator;

public class MathsStepDefinitions {

    int a = 0;
    int b = 0;
    int total = 0;

    Calculator calculator = new Calculator();

    @io.cucumber.java.en.Given("a is {int}")
    public void givenAIs(int value) {
        a = value;
    }

    @io.cucumber.java.en.Given("b is {int}")
    public void givenBIs(int value) {
        b = value;
    }

    @io.cucumber.java.en.When("I add a and b")
    public void iAddAAndB() {
        total = calculator.add(a,b);
    }

    @io.cucumber.java.en.Then("the total should be {int}")
    public void theTotalShouldBe(int expectedTotal) {
        assertThat(total).isEqualTo(expectedTotal);
    }

    @io.cucumber.java.en.Given("the first number is {int}")
    public void theFirstNumberIs(int arg0) {
        // check arg0
        if (arg0 != 0) {
            a = arg0;
        } else {
            throw new IllegalArgumentException("First number cannot be zero");
        }
    }

    @io.cucumber.java.en.And("the second number is {int}")
    public void theSecondNumberIs(int arg0) {
        b = arg0;
    }

    @io.cucumber.java.en.When("I add the two numbers")
    public void iAddTheTwoNumbers() {
       // perform addition
       total = calculator.add(a, b);
    }

    @io.cucumber.java.en.Then("the result should be {int}")
    public void theResultShouldBe(int arg0) {
        // verify result
        assertThat(total).isEqualTo(arg0);
    }
}
