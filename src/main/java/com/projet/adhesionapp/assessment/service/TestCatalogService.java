package com.projet.adhesionapp.assessment.service;


import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.repo.TestDefinitionRepository;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCatalogService {

    private final TestDefinitionRepository repo;

    public List<TestDefinition> findAll() {
        return repo.findAll();
    }
}