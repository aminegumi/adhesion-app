package com.projet.adhesionapp.assessment.service;

import com.projet.adhesionapp.assessment.domain.TestDefinition;
import com.projet.adhesionapp.assessment.model.CreateTestRequest;
import com.projet.adhesionapp.assessment.model.UpdateTestRequest;
import com.projet.adhesionapp.assessment.model.TestDto;
import com.projet.adhesionapp.assessment.repo.TestDefinitionRepository;
import com.projet.adhesionapp.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCatalogService {

    private final TestDefinitionRepository repo;

    public List<TestDefinition> findAll() {
        return repo.findAll();
    }

    public List<TestDto> findAllDto() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    public TestDto findById(Long id) {
        return toDto(repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Test id " + id + " introuvable")));
    }

    public TestDto findByCode(String code) {
        return toDto(repo.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Test code " + code + " introuvable")));
    }

    @Transactional
    public TestDto create(CreateTestRequest req) {
        if (repo.existsByCode(req.code())) {
            throw new IllegalArgumentException("Code déjà utilisé: " + req.code());
        }
        TestDefinition def = TestDefinition.builder()
                .code(req.code())
                .title(req.title())
                .version(req.version())
                .active(req.active())
                .build();
        return toDto(repo.save(def));
    }

    @Transactional
    public TestDto update(Long id, UpdateTestRequest req) {
        TestDefinition def = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Test id " + id + " introuvable"));
        def.setTitle(req.title());
        def.setVersion(req.version());
        def.setActive(req.active());
        return toDto(def); // persisted by dirty checking
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Test id " + id + " introuvable");
        }
        repo.deleteById(id);
    }

    private TestDto toDto(TestDefinition def) {
        return new TestDto(def.getId(), def.getCode(), def.getTitle(), def.getVersion(), def.isActive());
    }
}