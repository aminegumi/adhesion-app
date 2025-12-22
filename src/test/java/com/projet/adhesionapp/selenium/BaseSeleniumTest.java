package com.projet.adhesionapp.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Base class for Selenium E2E tests.
 * Provides common setup, teardown, and utility methods.
 */
public abstract class BaseSeleniumTest {

    protected static WebDriver driver;
    protected static WebDriverWait wait;
    
    // Configuration - change these based on your environment
    protected static final String ANGULAR_BASE_URL = "http://localhost:4200";
    protected static final String API_BASE_URL = "http://localhost:8082";
    protected static final int IMPLICIT_WAIT_SECONDS = 10;
    protected static final int EXPLICIT_WAIT_SECONDS = 20;
    
    // Test credentials
    protected static final String TEST_USER_EMAIL = "imadissame1@gmail.com";
    protected static final String TEST_USER_PASSWORD = "12341234";
    protected static final String ADMIN_EMAIL = "admin@gmail.com";
    protected static final String ADMIN_PASSWORD = "admin123";

    @BeforeAll
    static void setupClass() {
        // Setup Chrome driver (can be changed to Firefox)
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        // Uncomment for headless mode (CI/CD)
        // options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(IMPLICIT_WAIT_SECONDS));
        driver.manage().window().maximize();
        
        wait = new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT_SECONDS));
    }

    @AfterAll
    static void teardownClass() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void setup() {
        // Clear cookies and local storage before each test
        driver.manage().deleteAllCookies();
    }

    @AfterEach
    void teardown() {
        // Take screenshot on failure - handled by test listeners
    }

    /**
     * Navigate to the Angular application
     */
    protected void navigateToApp() {
        driver.get(ANGULAR_BASE_URL);
    }

    /**
     * Navigate to a specific path in the Angular application
     */
    protected void navigateTo(String path) {
        driver.get(ANGULAR_BASE_URL + path);
    }

    /**
     * Wait for page to load completely
     */
    protected void waitForPageLoad() {
        try {
            Thread.sleep(1000); // Allow Angular to bootstrap
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Clear local storage
     */
    protected void clearLocalStorage() {
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("window.localStorage.clear();");
    }
}
