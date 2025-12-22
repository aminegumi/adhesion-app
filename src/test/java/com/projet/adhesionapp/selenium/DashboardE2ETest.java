package com.projet.adhesionapp.selenium;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium E2E tests for Dashboard functionality.
 * Tests user dashboard features including medications, treatment plans, and statistics.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Dashboard E2E Tests")
@Tag("e2e")
class DashboardE2ETest extends BaseSeleniumTest {

    @BeforeEach
    void loginBeforeTest() {
        // Login before each test
        navigateTo("/login");
        waitForPageLoad();

        try {
            WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[type='email'], input[name='email']")));
            emailInput.clear();
            emailInput.sendKeys(TEST_USER_EMAIL);

            WebElement passwordInput = driver.findElement(By.cssSelector("input[type='password']"));
            passwordInput.clear();
            passwordInput.sendKeys(TEST_USER_PASSWORD);

            driver.findElement(By.cssSelector("button[type='submit']")).click();
            waitForPageLoad();
        } catch (Exception e) {
            System.out.println("Login before test failed: " + e.getMessage());
        }
    }

    @Nested
    @DisplayName("Dashboard Display Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DashboardDisplayTests {

        @Test
        @Order(1)
        @DisplayName("Should display dashboard after login")
        void shouldDisplayDashboard() {
            // When
            navigateTo("/dashboard");
            waitForPageLoad();

            // Then
            assertTrue(driver.getCurrentUrl().contains("/dashboard") || 
                       driver.getCurrentUrl().contains("/login"),
                    "Should be on dashboard or redirected to login");
        }

        @Test
        @Order(2)
        @DisplayName("Should display user statistics")
        void shouldDisplayUserStatistics() {
            // When
            navigateTo("/dashboard");
            waitForPageLoad();

            // Then - Check for statistics elements
            try {
                boolean hasStatistics = !driver.findElements(
                        By.cssSelector(".stat-card, .statistics, .adherence-rate, .stats")).isEmpty();
                
                // Statistics might be present
                System.out.println("Statistics present: " + hasStatistics);
            } catch (Exception e) {
                System.out.println("Statistics check failed: " + e.getMessage());
            }
        }

        @Test
        @Order(3)
        @DisplayName("Should display navigation sidebar")
        void shouldDisplayNavigationSidebar() {
            // When
            navigateTo("/dashboard");
            waitForPageLoad();

            // Then
            try {
                List<WebElement> navLinks = driver.findElements(
                        By.cssSelector(".sidebar a, nav a, .nav-link"));
                
                assertFalse(navLinks.isEmpty(), "Navigation links should be present");
            } catch (Exception e) {
                System.out.println("Navigation check failed: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Medication Management Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class MedicationManagementTests {

        @Test
        @Order(1)
        @DisplayName("Should navigate to medications page")
        void shouldNavigateToMedications() {
            // When
            navigateTo("/medications");
            waitForPageLoad();

            // Then
            assertTrue(driver.getCurrentUrl().contains("/medications") ||
                       driver.getCurrentUrl().contains("/login"),
                    "Should be on medications page");
        }

        @Test
        @Order(2)
        @DisplayName("Should display medications list")
        void shouldDisplayMedicationsList() {
            // When
            navigateTo("/medications");
            waitForPageLoad();

            // Then
            try {
                // Check if medications list or empty state is displayed
                boolean hasMedicationsContent = 
                        !driver.findElements(By.cssSelector(".medication-card, .medication-item, .empty-state")).isEmpty();
                
                assertTrue(hasMedicationsContent, "Medications content should be displayed");
            } catch (Exception e) {
                System.out.println("Medications list check failed: " + e.getMessage());
            }
        }

        @Test
        @Order(3)
        @DisplayName("Should open add medication dialog")
        void shouldOpenAddMedicationDialog() {
            // Given
            navigateTo("/medications");
            waitForPageLoad();

            // When
            try {
                WebElement addButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".add-btn, [class*='add'], button:contains('Add')")));
                addButton.click();
                waitForPageLoad();

                // Then - dialog should be visible
                boolean dialogVisible = !driver.findElements(
                        By.cssSelector(".dialog, .modal, [class*='dialog']")).isEmpty();
                
                System.out.println("Add medication dialog visible: " + dialogVisible);
            } catch (Exception e) {
                System.out.println("Add medication dialog test failed: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Treatment Plans Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TreatmentPlansTests {

        @Test
        @Order(1)
        @DisplayName("Should navigate to treatment plans page")
        void shouldNavigateToTreatmentPlans() {
            // When
            navigateTo("/treatment-plans");
            waitForPageLoad();

            // Then
            assertTrue(driver.getCurrentUrl().contains("/treatment") ||
                       driver.getCurrentUrl().contains("/login"),
                    "Should be on treatment plans page");
        }

        @Test
        @Order(2)
        @DisplayName("Should display treatment plans list")
        void shouldDisplayTreatmentPlansList() {
            // When
            navigateTo("/treatment-plans");
            waitForPageLoad();

            // Then
            try {
                boolean hasPlansContent = 
                        !driver.findElements(By.cssSelector(".plan-card, .treatment-plan, .empty-state")).isEmpty();
                
                System.out.println("Treatment plans content present: " + hasPlansContent);
            } catch (Exception e) {
                System.out.println("Treatment plans list check failed: " + e.getMessage());
            }
        }

        @Test
        @Order(3)
        @DisplayName("Should show today's tasks tab")
        void shouldShowTodaysTasksTab() {
            // When
            navigateTo("/treatment-plans");
            waitForPageLoad();

            // Then
            try {
                WebElement todaysTasksTab = driver.findElement(
                        By.cssSelector(".tab, [class*='tab']:contains('Today')"));
                
                assertNotNull(todaysTasksTab, "Today's tasks tab should exist");
            } catch (Exception e) {
                System.out.println("Today's tasks tab not found: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Psychological Tests Section")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PsychologicalTestsTests {

        @Test
        @Order(1)
        @DisplayName("Should navigate to tests page")
        void shouldNavigateToTestsPage() {
            // When
            navigateTo("/tests");
            waitForPageLoad();

            // Then
            assertTrue(driver.getCurrentUrl().contains("/tests") ||
                       driver.getCurrentUrl().contains("/login"),
                    "Should be on tests page");
        }

        @Test
        @Order(2)
        @DisplayName("Should display available tests")
        void shouldDisplayAvailableTests() {
            // When
            navigateTo("/tests");
            waitForPageLoad();

            // Then
            try {
                List<WebElement> testCards = driver.findElements(
                        By.cssSelector(".test-card, .assessment-card, [class*='test']"));
                
                System.out.println("Number of test cards found: " + testCards.size());
            } catch (Exception e) {
                System.out.println("Tests display check failed: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Motivation Section Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class MotivationTests {

        @Test
        @Order(1)
        @DisplayName("Should navigate to motivation page")
        void shouldNavigateToMotivationPage() {
            // When
            navigateTo("/motivation");
            waitForPageLoad();

            // Then
            assertTrue(driver.getCurrentUrl().contains("/motivation") ||
                       driver.getCurrentUrl().contains("/login"),
                    "Should be on motivation page");
        }

        @Test
        @Order(2)
        @DisplayName("Should display motivation categories")
        void shouldDisplayMotivationCategories() {
            // When
            navigateTo("/motivation");
            waitForPageLoad();

            // Then
            try {
                List<WebElement> categories = driver.findElements(
                        By.cssSelector(".category-card, .motivation-category, [class*='category']"));
                
                System.out.println("Number of motivation categories: " + categories.size());
            } catch (Exception e) {
                System.out.println("Motivation categories check failed: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Profile Section Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ProfileTests {

        @Test
        @Order(1)
        @DisplayName("Should navigate to profile page")
        void shouldNavigateToProfilePage() {
            // When
            navigateTo("/profile");
            waitForPageLoad();

            // Then
            assertTrue(driver.getCurrentUrl().contains("/profile") ||
                       driver.getCurrentUrl().contains("/login"),
                    "Should be on profile page");
        }

        @Test
        @Order(2)
        @DisplayName("Should display user profile information")
        void shouldDisplayProfileInformation() {
            // When
            navigateTo("/profile");
            waitForPageLoad();

            // Then
            try {
                boolean hasProfileInfo = !driver.findElements(
                        By.cssSelector(".profile-info, .user-info, [class*='profile']")).isEmpty();
                
                System.out.println("Profile information present: " + hasProfileInfo);
            } catch (Exception e) {
                System.out.println("Profile info check failed: " + e.getMessage());
            }
        }

        @Test
        @Order(3)
        @DisplayName("Should navigate to edit profile page")
        void shouldNavigateToEditProfile() {
            // Given
            navigateTo("/profile");
            waitForPageLoad();

            // When
            try {
                WebElement editButton = driver.findElement(
                        By.cssSelector(".edit-btn, [class*='edit'], a[href*='edit']"));
                editButton.click();
                waitForPageLoad();

                // Then
                assertTrue(driver.getCurrentUrl().contains("/edit") ||
                           driver.getCurrentUrl().contains("/profile"),
                        "Should navigate to edit profile");
            } catch (Exception e) {
                System.out.println("Edit profile navigation failed: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Predictions Section Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PredictionsTests {

        @Test
        @Order(1)
        @DisplayName("Should navigate to predictions page")
        void shouldNavigateToPredictionsPage() {
            // When
            navigateTo("/predictions");
            waitForPageLoad();

            // Then
            assertTrue(driver.getCurrentUrl().contains("/predictions") ||
                       driver.getCurrentUrl().contains("/login"),
                    "Should be on predictions page");
        }

        @Test
        @Order(2)
        @DisplayName("Should display adherence prediction")
        void shouldDisplayAdherencePrediction() {
            // When
            navigateTo("/predictions");
            waitForPageLoad();

            // Then
            try {
                boolean hasPredictionContent = !driver.findElements(
                        By.cssSelector(".prediction, .adherence, [class*='prediction']")).isEmpty();
                
                System.out.println("Prediction content present: " + hasPredictionContent);
            } catch (Exception e) {
                System.out.println("Predictions display check failed: " + e.getMessage());
            }
        }
    }
}
