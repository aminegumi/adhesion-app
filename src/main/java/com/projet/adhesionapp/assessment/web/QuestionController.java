package com.projet.adhesionapp.assessment.web;

import com.projet.adhesionapp.assessment.model.CreateQuestionRequest;
import com.projet.adhesionapp.assessment.model.UpdateQuestionRequest;
import com.projet.adhesionapp.assessment.model.QuestionItemDto;
import com.projet.adhesionapp.assessment.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService service;

    @GetMapping("/test/{testId}")
    public List<QuestionItemDto> getByTest(@PathVariable Long testId) {
        return service.getByTestId(testId);
    }

    @GetMapping("/{id}")
    public QuestionItemDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionItemDto create(@Valid @RequestBody CreateQuestionRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public QuestionItemDto update(@PathVariable Long id, @Valid @RequestBody UpdateQuestionRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}