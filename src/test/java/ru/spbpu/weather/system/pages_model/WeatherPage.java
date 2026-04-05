package ru.spbpu.weather.system.pages_model;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class WeatherPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(name = "city")
    private WebElement cityInput;

    @FindBy(css = "button[type='submit'], input[type='submit']")
    private WebElement searchButton;

    @FindBy(css = ".weather-card, .current-weather")
    private WebElement weatherCard;

    @FindBy(css = ".forecast-card")
    private List<WebElement> forecastCards;

    @FindBy(css = ".user-menu, .user-btn")
    private WebElement userMenu;

    @FindBy(linkText = "History")
    private WebElement historyLink;

    @FindBy(css = ".error-message, .error-container")
    private WebElement errorMessage;

    @FindBy(css = ".history-item, .request-item")
    private List<WebElement> historyItems;

    public WeatherPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get(driver.getCurrentUrl().replace("/auth/login", "/weather"));
    }

    public void searchCity(String city) {
        wait.until(ExpectedConditions.visibilityOf(cityInput));
        cityInput.clear();
        cityInput.sendKeys(city);
        searchButton.click();
    }

    public boolean isWeatherDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(weatherCard)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentTemperature() {
        WebElement temp = wait.until(ExpectedConditions.visibilityOf(weatherCard));
        return temp.getText();
    }

    public int getForecastCount() {
        try {
            return forecastCards.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isErrorDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(errorMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void openHistory() {
        wait.until(ExpectedConditions.elementToBeClickable(userMenu)).click();
        wait.until(ExpectedConditions.elementToBeClickable(historyLink)).click();
    }

    public int getHistoryItemsCount() {
        try {
            wait.until(ExpectedConditions.visibilityOfAllElements(historyItems));
            return historyItems.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isHistoryContainsCity(String city) {
        try {
            return historyItems.stream()
                    .anyMatch(item -> item.getText().contains(city));
        } catch (Exception e) {
            return false;
        }
    }

    public void logout() {
        wait.until(ExpectedConditions.elementToBeClickable(userMenu)).click();
        WebElement logoutBtn = wait.until(ExpectedConditions.elementToBeClickable(
                org.openqa.selenium.By.cssSelector("input[value='Logout'], button:contains('Logout')")));
        logoutBtn.click();
    }
}