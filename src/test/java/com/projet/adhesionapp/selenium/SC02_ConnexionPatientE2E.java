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
 * SC-02 : Connexion d'un patient existant
 * 
 * Étapes :
 * - Navigation vers la page de connexion
 * - Saisie des identifiants valides
 * - Vérification de l'accès au dashboard patient
 */
@DisplayName("SC-02 : Connexion d'un patient existant")
class SC02_ConnexionPatientE2E {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:4200";
    private static final String PATIENT_EMAIL = "imadissame1@gmail.com";
    private static final String PATIENT_PASSWORD = "12341234";

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless");
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
    @DisplayName("Connexion avec identifiants valides")
    void testConnexionPatientExistant() {
        // 1. Navigation vers la page de connexion
        driver.get(BASE_URL + "/login");
        
        // 2. Attendre que la page soit chargée
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));
        
        // 3. Vérifier que la page de connexion est affichée
        assertTrue(driver.getCurrentUrl().contains("login"), 
                "La page de connexion doit être affichée");

        // 4. Saisie des identifiants valides
        WebElement emailInput = driver.findElement(By.name("email"));
        emailInput.sendKeys(PATIENT_EMAIL);
        
        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys(PATIENT_PASSWORD);

        // 5. Clic sur le bouton de connexion
        WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit']"));
        submitButton.click();

        // 6. Attendre la redirection vers le dashboard
        wait.until(ExpectedConditions.urlContains("dashboard"));

        // 7. Vérification de l'accès au dashboard patient
        assertTrue(driver.getCurrentUrl().contains("dashboard"),
                "L'utilisateur doit être redirigé vers le dashboard");
    }
}
