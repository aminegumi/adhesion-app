package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.model.*;
import com.projet.adhesionapp.assessment.repo.TestDefinitionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestCatalogService - Tests unitaires")
class TestCatalogServiceTest {

    @Mock private TestDefinitionRepository repo;
    @InjectMocks private TestCatalogService service;

    private TestDefinition testDef;

    @BeforeEach
    void setUp() {
        testDef = TestDefinition.builder()
                .id(1L).code("PHQ9").title("PHQ-9").version("1.0").active(true).build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {
        @Test
        void shouldReturnAllTests() {
            when(repo.findAll()).thenReturn(List.of(testDef));
            List<TestDefinition> result = service.findAll();
            assertThat(result).hasSize(1);
        }

        @Test
        void shouldReturnAllDtos() {
            when(repo.findAll()).thenReturn(List.of(testDef));
            List<TestDto> result = service.findAllDto();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("PHQ9");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {
        @Test
        void shouldReturnTest() {
            when(repo.findById(1L)).thenReturn(Optional.of(testDef));
            TestDto result = service.findById(1L);
            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(repo.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByCode")
    class FindByCodeTests {
        @Test
        void shouldReturnTestByCode() {
            when(repo.findByCode("PHQ9")).thenReturn(Optional.of(testDef));
            TestDto result = service.findByCode("PHQ9");
            assertThat(result.code()).isEqualTo("PHQ9");
        }

        @Test
        void shouldThrowWhenCodeNotFound() {
            when(repo.findByCode("INVALID")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findByCode("INVALID"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {
        @Test
        void shouldCreateTest() {
            CreateTestRequest request = new CreateTestRequest("GAD7", "GAD-7", "1.0", true);
            when(repo.existsByCode("GAD7")).thenReturn(false);
            when(repo.save(any(TestDefinition.class))).thenAnswer(inv -> {
                TestDefinition t = inv.getArgument(0);
                t.setId(2L);
                return t;
            });

            TestDto result = service.create(request);
            assertThat(result.code()).isEqualTo("GAD7");
        }

        @Test
        void shouldThrowWhenCodeExists() {
            CreateTestRequest request = new CreateTestRequest("PHQ9", "PHQ-9", "1.0", true);
            when(repo.existsByCode("PHQ9")).thenReturn(true);
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Code déjà utilisé");
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {
        @Test
        void shouldUpdateTest() {
            UpdateTestRequest request = new UpdateTestRequest("PHQ-9 Updated", "2.0", true);
            when(repo.findById(1L)).thenReturn(Optional.of(testDef));

            TestDto result = service.update(1L, request);
            assertThat(result.title()).isEqualTo("PHQ-9 Updated");
            assertThat(result.version()).isEqualTo("2.0");
        }

        @Test
        void shouldThrowWhenNotFound() {
            UpdateTestRequest request = new UpdateTestRequest("Test", "1.0", true);
            when(repo.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(999L, request))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {
        @Test
        void shouldDeleteTest() {
            when(repo.existsById(1L)).thenReturn(true);
            doNothing().when(repo).deleteById(1L);
            service.delete(1L);
            verify(repo).deleteById(1L);
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(repo.existsById(999L)).thenReturn(false);
            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
