package ru.spbpu.weather.system.pages_model;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(css = "form input[name='username']")
    private WebElement usernameInput;

    @FindBy(css = "form input[name='password']")
    private WebElement passwordInput;

    @FindBy(css = "form button[type='submit'], form input[type='submit']")
    private WebElement submitButton;

    @FindBy(css = ".error-message, .alert-danger")
    private WebElement errorMessage;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get(driver.getCurrentUrl().replace("/weather", "/auth/login"));
    }

    public void login(String username, String password) {
        wait.until(ExpectedConditions.visibilityOf(usernameInput));
        usernameInput.clear();
        usernameInput.sendKeys(username);
        passwordInput.clear();
        passwordInput.sendKeys(password);
        submitButton.click();
    }

    public boolean isErrorMessageDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(errorMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getErrorMessage() {
        return errorMessage.getText();
    }

    public void goToRegistration() {
        registerLink.click();
    }
}