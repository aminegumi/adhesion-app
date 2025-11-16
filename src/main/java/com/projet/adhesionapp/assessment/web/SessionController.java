package com.projet.adhesionapp.assessment.web;


import com.projet.adhesionapp.assessment.domain.Answer;
import com.projet.adhesionapp.assessment.domain.QuestionItem;
import com.projet.adhesionapp.assessment.domain.TestSession;
import com.projet.adhesionapp.assessment.model.AnswerUpsertRequest;
import com.projet.adhesionapp.assessment.model.StartSessionRequest;
import com.projet.adhesionapp.assessment.service.TestSessionService;
import com.projet.adhesionapp.identity.domain.User;
import com.projet.adhesionapp.identity.service.UserService;
import lombok.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final TestSessionService service;
    private final UserService userService;

    @PostMapping("/start")
    public TestSession start(@RequestBody StartSessionRequest req) {
        User user = userService.findById(req.userId());
        return service.startSession(req.testId(), user);
    }

    @PostMapping("/{id}/submit")
    public void submit(@PathVariable Long id,
                       @RequestBody List<AnswerUpsertRequest> answers) {

        List<Answer> entities = answers.stream()
                .map(a -> Answer.builder()
                        .value(a.value())
                        .item(QuestionItem.builder().id(a.itemId()).build())
                        .build())
                .toList();

        service.submitAnswers(id, entities);
    }
}
