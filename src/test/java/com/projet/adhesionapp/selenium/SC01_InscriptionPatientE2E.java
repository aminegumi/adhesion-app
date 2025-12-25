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
 * SC-01 : Inscription d'un nouveau patient
 * 
 * Étapes :
 * - Navigation vers la page d'inscription
 * - Saisie des informations (nom, email, mot de passe)
 * - Validation du formulaire
 * - Vérification de la redirection vers le dashboard
 */
@DisplayName("SC-01 : Inscription d'un nouveau patient")
class SC01_InscriptionPatientE2E {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:4200";

    @BeforeEach
    void setUp() {
        // Configuration du driver Chrome
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        //.addArguments("--headless"); // Mode sans interface graphique
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
    @DisplayName("Inscription avec informations valides")
    void testInscriptionNouveauPatient() {
        // 1. Navigation vers la page d'inscription
        driver.get(BASE_URL + "/register");
        
        // 2. Attendre que la page soit chargée
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));
        
        // 3. Vérifier que la page d'inscription est affichée
        assertTrue(driver.getCurrentUrl().contains("register"), 
                "La page d'inscription doit être affichée");

        // 4. Saisie des informations
        String emailUnique = "patient" + System.currentTimeMillis() + "@test.com";
        
        WebElement nomInput = driver.findElement(By.name("displayName"));
        nomInput.sendKeys("Nouveau Patient Test");

        WebElement emailInput = driver.findElement(By.name("email"));
        emailInput.sendKeys(emailUnique);

        // Date de naissance
        WebElement birthDateInput = driver.findElement(By.name("birthDate"));
        birthDateInput.sendKeys("1990-01-01");

        // Genre
        WebElement genderSelect = driver.findElement(By.name("gender"));
        genderSelect.sendKeys("Male");

        WebElement passwordInput = driver.findElement(By.name("password"));
        passwordInput.sendKeys("Password123!");

        // Consentement
        WebElement consentCheckbox = driver.findElement(By.name("consent"));
        if (!consentCheckbox.isSelected()) {
            consentCheckbox.click();
        }

        // 5. Validation du formulaire
        WebElement submitButton = driver.findElement(By.cssSelector("button[type='submit']"));
        submitButton.click();

        // 6. Attendre la redirection (vers login ou dashboard selon la logique d'inscription)
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("dashboard"),
            ExpectedConditions.urlContains("login")
        ));

        // 7. Vérification de la redirection vers le dashboard ou login (après inscription)
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("dashboard") || currentUrl.contains("login") || currentUrl.contains("onboarding"),
                "L'utilisateur doit être redirigé vers le dashboard, login ou onboarding après inscription");
    }
}
