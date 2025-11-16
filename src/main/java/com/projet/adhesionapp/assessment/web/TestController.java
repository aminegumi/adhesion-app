package com.projet.adhesionapp.assessment.web;


import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.service.TestCatalogService;
import lombok.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestController {

    private final TestCatalogService service;

    @GetMapping
    public List<TestDefinition> getTests() {
        return service.findAll();
    }
}
