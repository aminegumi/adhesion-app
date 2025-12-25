package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.QuestionItem;
import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.model.*;
import com.projet.adhesionapp.assessment.repo.QuestionItemRepository;
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
@DisplayName("QuestionService - Tests unitaires")
class QuestionServiceTest {

    @Mock private QuestionItemRepository repo;
    @Mock private TestDefinitionRepository testRepo;
    @InjectMocks private QuestionService service;

    private QuestionItem testQuestion;
    private TestDefinition testDefinition;

    @BeforeEach
    void setUp() {
        testDefinition = TestDefinition.builder()
                .id(1L).code("PHQ9").title("PHQ-9").version("1.0").active(true).build();
        
        testQuestion = QuestionItem.builder()
                .id(1L).code("Q1").text("Question 1").orderIndex(1)
                .minScore(0).maxScore(3).reverseScored(false)
                .testDefinition(testDefinition).build();
    }

    @Nested
    @DisplayName("getByTestId")
    class GetByTestIdTests {
        @Test
        void shouldReturnQuestionsForTest() {
            when(repo.findByTestDefinitionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(testQuestion));
            List<QuestionItemDto> result = service.getByTestId(1L);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("Q1");
        }

        @Test
        void shouldReturnEmptyListWhenNoQuestions() {
            when(repo.findByTestDefinitionIdOrderByOrderIndexAsc(999L)).thenReturn(List.of());
            List<QuestionItemDto> result = service.getByTestId(999L);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {
        @Test
        void shouldReturnQuestion() {
            when(repo.findById(1L)).thenReturn(Optional.of(testQuestion));
            QuestionItemDto result = service.getById(1L);
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.text()).isEqualTo("Question 1");
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(repo.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {
        @Test
        void shouldCreateQuestion() {
            CreateQuestionRequest request = new CreateQuestionRequest(1L, "Q2", "Question 2", 1, false, 0, 3);
            when(testRepo.findById(1L)).thenReturn(Optional.of(testDefinition));
            when(repo.save(any(QuestionItem.class))).thenAnswer(inv -> {
                QuestionItem q = inv.getArgument(0);
                q.setId(2L);
                return q;
            });

            QuestionItemDto result = service.create(request);
            assertThat(result.code()).isEqualTo("Q2");
            verify(repo).save(any(QuestionItem.class));
        }

        @Test
        void shouldThrowWhenTestNotFound() {
            CreateQuestionRequest request = new CreateQuestionRequest(999L, "Q2", "Question 2", 1, false, 0, 3);
            when(testRepo.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {
        @Test
        void shouldUpdateQuestion() {
            UpdateQuestionRequest request = new UpdateQuestionRequest("Updated", 2, true, 0, 4);
            when(repo.findById(1L)).thenReturn(Optional.of(testQuestion));

            QuestionItemDto result = service.update(1L, request);
            assertThat(result.text()).isEqualTo("Updated");
            assertThat(result.orderIndex()).isEqualTo(2);
        }

        @Test
        void shouldThrowWhenQuestionNotFound() {
            UpdateQuestionRequest request = new UpdateQuestionRequest("Updated", 2, true, 0, 4);
            when(repo.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(999L, request))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {
        @Test
        void shouldDeleteQuestion() {
            when(repo.existsById(1L)).thenReturn(true);
            doNothing().when(repo).deleteById(1L);
            service.delete(1L);
            verify(repo).deleteById(1L);
        }

        @Test
        void shouldThrowWhenQuestionNotFound() {
            when(repo.existsById(999L)).thenReturn(false);
            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
