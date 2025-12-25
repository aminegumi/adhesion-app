package com.projet.adhesionapp.assessment.web;

import com.projet.adhesionapp.assessment.model.*;
import com.projet.adhesionapp.assessment.service.TestCatalogService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du contrôleur de tests psychologiques.
 * CT_TEST_01 - CT_TEST_05: CRUD et catalogue des tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Test Controller - Tests unitaires")
class TestControllerTest {

    @Mock private TestCatalogService testCatalogService;
    @InjectMocks private TestController controller;

    private TestDto testDto;

    @BeforeEach
    void setUp() {
        // TestDto(Long id, String code, String title, String version, boolean active)
        testDto = new TestDto(1L, "PHQ9", "Patient Health Questionnaire-9", "1.0", true);
    }

    @Nested
    @DisplayName("CT_TEST_01: Liste des tests")
    class ListTestsTests {

        @Test
        @DisplayName("CT_TEST_01a: Récupère tous les tests")
        void shouldGetAllTests() {
            when(testCatalogService.findAllDto()).thenReturn(List.of(testDto));

            List<TestDto> result = controller.getTests();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("PHQ9");
        }
    }

    @Nested
    @DisplayName("CT_TEST_02: Récupération par ID/Code")
    class GetByIdCodeTests {

        @Test
        @DisplayName("CT_TEST_02a: Récupère un test par ID")
        void shouldGetTestById() {
            when(testCatalogService.findById(1L)).thenReturn(testDto);

            TestDto result = controller.getById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("Patient Health Questionnaire-9");
        }

        @Test
        @DisplayName("CT_TEST_02b: Récupère un test par code")
        void shouldGetTestByCode() {
            when(testCatalogService.findByCode("PHQ9")).thenReturn(testDto);

            TestDto result = controller.getByCode("PHQ9");

            assertThat(result.code()).isEqualTo("PHQ9");
        }
    }

    @Nested
    @DisplayName("CT_TEST_03: Création de tests")
    class CreateTestTests {

        @Test
        @DisplayName("CT_TEST_03a: Crée un nouveau test")
        void shouldCreateTest() {
            // CreateTestRequest(String code, String title, String version, boolean active)
            CreateTestRequest request = new CreateTestRequest("GAD7", "Anxiety Scale", "1.0", true);
            TestDto newDto = new TestDto(2L, "GAD7", "Anxiety Scale", "1.0", true);

            when(testCatalogService.create(request)).thenReturn(newDto);

            TestDto result = controller.create(request);

            assertThat(result.code()).isEqualTo("GAD7");
            assertThat(result.active()).isTrue();
        }
    }

    @Nested
    @DisplayName("CT_TEST_04: Mise à jour de tests")
    class UpdateTestTests {

        @Test
        @DisplayName("CT_TEST_04a: Met à jour un test existant")
        void shouldUpdateTest() {
            // UpdateTestRequest(String title, String version, boolean active)
            UpdateTestRequest request = new UpdateTestRequest("PHQ-9 Updated", "2.0", true);
            TestDto updatedDto = new TestDto(1L, "PHQ9", "PHQ-9 Updated", "2.0", true);

            when(testCatalogService.update(1L, request)).thenReturn(updatedDto);

            TestDto result = controller.update(1L, request);

            assertThat(result.version()).isEqualTo("2.0");
            assertThat(result.title()).isEqualTo("PHQ-9 Updated");
        }
    }

    @Nested
    @DisplayName("CT_TEST_05: Suppression de tests")
    class DeleteTestTests {

        @Test
        @DisplayName("CT_TEST_05a: Supprime un test")
        void shouldDeleteTest() {
            doNothing().when(testCatalogService).delete(1L);

            controller.delete(1L);

            verify(testCatalogService).delete(1L);
        }
    }
}
