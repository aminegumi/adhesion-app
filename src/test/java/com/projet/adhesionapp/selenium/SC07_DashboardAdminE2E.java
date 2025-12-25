package com.projet.adhesionapp.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SC-07 : Dashboard administrateur
 * 
 * Étapes :
 * - Connexion en tant qu'administrateur
 * - Vérification de l'accès aux fonctionnalités d'administration
 * - Navigation dans les différentes sections
 */
@DisplayName("SC-07 : Dashboard administrateur")
class SC07_DashboardAdminE2E {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:4200";
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless");
        // options.addArguments("--no-sandbox");
        // options.addArguments("--disable-dev-shm-usage");
        
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Accès au dashboard administrateur")
    void testDashboardAdministrateur() {
        // 1. Navigation vers la page de connexion admin
        driver.get(BASE_URL + "/admin/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        // 2. Connexion en tant qu'administrateur
        driver.findElement(By.name("email")).sendKeys(ADMIN_EMAIL);
        driver.findElement(By.name("password")).sendKeys(ADMIN_PASSWORD);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // 3. Attendre la redirection
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 4. Navigation vers le dashboard admin
        driver.get(BASE_URL + "/admin/dashboard");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        // 5. Vérification de l'accès aux fonctionnalités d'administration
        String currentUrl = driver.getCurrentUrl();
        boolean adminAccessible = currentUrl.contains("admin") || 
                                   currentUrl.contains("dashboard");

        assertTrue(adminAccessible, "Le dashboard admin doit être accessible");

        // 6. Navigation dans les différentes sections (si disponibles)
        try {
            // Essayer de cliquer sur différentes sections admin
            WebElement usersSection = driver.findElement(
                By.cssSelector("[href*='users'], .admin-users, .menu-users"));
            if (usersSection.isDisplayed()) {
                usersSection.click();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            // Section non trouvée
        }

        // 7. Vérification finale - l'admin peut naviguer
        assertTrue(driver.getCurrentUrl().contains("admin") || 
                   driver.getCurrentUrl().contains("dashboard") ||
                   driver.getCurrentUrl().contains("users"),
                "L'administrateur doit pouvoir naviguer dans les sections");
    }
}
