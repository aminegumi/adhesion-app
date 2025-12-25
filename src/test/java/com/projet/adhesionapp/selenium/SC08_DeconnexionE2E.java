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
 * SC-08 : Déconnexion
 * 
 * Étapes :
 * - Clic sur le bouton de déconnexion
 * - Vérification de la redirection vers la page de connexion
 * - Vérification de l'impossibilité d'accéder aux pages protégées
 */
@DisplayName("SC-08 : Déconnexion")
class SC08_DeconnexionE2E {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:4200";
    private static final String PATIENT_EMAIL = "imadissame1@gmail.com";
    private static final String PATIENT_PASSWORD = "12341234";

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
    @DisplayName("Déconnexion et protection des pages")
    void testDeconnexion() {
        // 1. Connexion préalable
        driver.get(BASE_URL + "/login");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));
        
        driver.findElement(By.name("email")).sendKeys(PATIENT_EMAIL);
        driver.findElement(By.name("password")).sendKeys(PATIENT_PASSWORD);
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        
        wait.until(ExpectedConditions.urlContains("dashboard"));

        // 2. Clic sur le bouton de déconnexion
        boolean logoutClicked = false;
        try {
            WebElement logoutButton = driver.findElement(
                By.cssSelector(".logout-btn, [data-action='logout'], #logout, .logout, button[onclick*='logout']"));
            logoutButton.click();
            logoutClicked = true;
            Thread.sleep(1000);
        } catch (Exception e) {
            // Bouton logout non trouvé, essayer navigation directe
        }

        // Alternative : navigation directe vers logout
        if (!logoutClicked) {
            driver.get(BASE_URL + "/logout");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 3. Vérification de la redirection vers la page de connexion
        String currentUrl = driver.getCurrentUrl();
        boolean redirectionOk = currentUrl.contains("login") || 
                                 currentUrl.contains("home") ||
                                 currentUrl.equals(BASE_URL + "/") ||
                                 currentUrl.equals(BASE_URL);

        assertTrue(redirectionOk,
                "L'utilisateur doit être redirigé vers login ou page d'accueil après déconnexion");

        // 4. Vérification de l'impossibilité d'accéder aux pages protégées
        driver.get(BASE_URL + "/dashboard");
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. Doit être redirigé vers login ou page d'accueil (pas accès au dashboard)
        String urlApresAccesDashboard = driver.getCurrentUrl();
        boolean pagesProtegeesInaccessibles = urlApresAccesDashboard.contains("login") || 
                                                urlApresAccesDashboard.contains("home") ||
                                                !urlApresAccesDashboard.contains("dashboard");

        assertTrue(pagesProtegeesInaccessibles,
                "Les pages protégées doivent être inaccessibles après déconnexion");
    }
}
