package com.projet.adhesionapp.assessment.web;

import com.projet.adhesionapp.assessment.model.TestDto;
import com.projet.adhesionapp.assessment.model.CreateTestRequest;
import com.projet.adhesionapp.assessment.model.UpdateTestRequest;
import com.projet.adhesionapp.assessment.service.TestCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestController {

    private final TestCatalogService service;

    @GetMapping
    public List<TestDto> getTests() {
        return service.findAllDto();
    }

    @GetMapping("/{id}")
    public TestDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/code/{code}")
    public TestDto getByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestDto create(@Valid @RequestBody CreateTestRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public TestDto update(@PathVariable Long id, @Valid @RequestBody UpdateTestRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
