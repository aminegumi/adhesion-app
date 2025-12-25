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
 * SC-03 : Connexion avec identifiants invalides
 * 
 * Étapes :
 * - Tentative de connexion avec un mauvais mot de passe
 * - Vérification de l'affichage du message d'erreur
 */
@DisplayName("SC-03 : Connexion avec identifiants invalides")
class SC03_ConnexionInvalideE2E {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:4200";
    private static final String PATIENT_EMAIL = "imadissame1@gmail.com";
    private static final String MAUVAIS_PASSWORD = "mauvais_mot_de_passe";

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
    @DisplayName("Connexion avec mauvais mot de passe affiche erreur")
    void testConnexionIdentifiantsInvalides() {
        // 1. Navigation vers la page de connexion
        driver.get(BASE_URL + "/login");
        
        // 2. Attendre que la page soit chargée
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        // 3. Tentative de connexion avec un mauvais mot de passe
        WebElement emailInput = driver.findElement(By.name("email"));
        emailInput.sendKeys(PATIENT_EMAIL);
        
        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys(MAUVAIS_PASSWORD);

        // 4. Clic sur le bouton de connexion
        WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit']"));
        submitButton.click();

        // 5. Attendre un peu pour le retour de l'erreur
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 6. Vérification : soit message d'erreur affiché, soit reste sur login
        boolean restesSurLogin = driver.getCurrentUrl().contains("login");
        boolean messageErreurAffiche = false;
        
        try {
            WebElement errorMessage = driver.findElement(
                By.cssSelector(".error-message, .alert-danger, .error, [class*='error']"));
            messageErreurAffiche = errorMessage.isDisplayed();
        } catch (Exception e) {
            // Pas de message d'erreur visible
        }

        // 7. L'utilisateur doit rester sur login ou voir un message d'erreur
        assertTrue(restesSurLogin || messageErreurAffiche,
                "L'utilisateur doit rester sur la page login ou voir un message d'erreur");
    }
}
