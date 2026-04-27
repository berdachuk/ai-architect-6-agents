package com.berdachuk.meteoris.insight.api;

import com.berdachuk.meteoris.insight.agent.api.ChatTurnResult;
import com.berdachuk.meteoris.insight.agent.api.ChatOrchestration;
import com.berdachuk.meteoris.insight.core.IdGenerator;
import com.berdachuk.meteoris.insight.evaluation.EvaluationService;
import com.berdachuk.meteoris.insight.memory.TodoStateStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@ConditionalOnWebApplication
public class SiteController {

    private final ChatOrchestration orchestrationService;
    private final EvaluationService evaluationService;
    private final IdGenerator idGenerator;
    private final Environment environment;
    private final TodoStateStore todoStateStore;

    public SiteController(
            ChatOrchestration orchestrationService,
            EvaluationService evaluationService,
            IdGenerator idGenerator,
            Environment environment,
            TodoStateStore todoStateStore) {
        this.orchestrationService = orchestrationService;
        this.evaluationService = evaluationService;
        this.idGenerator = idGenerator;
        this.environment = environment;
        this.todoStateStore = todoStateStore;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("profiles", String.join(",", environment.getActiveProfiles()));
        return "home";
    }

    @GetMapping("/chat")
    public String chatGet(
            Model model, HttpServletRequest request, HttpServletResponse response, @RequestParam(required = false) String reply) {
        String sid = SessionCookieSupport.readOrCreate(request, response, idGenerator);
        model.addAttribute("sessionId", sid);
        model.addAttribute("reply", reply);
        model.addAttribute("profiles", String.join(",", environment.getActiveProfiles()));
        model.addAttribute("todos", todoStateStore.list(sid));
        return "chat";
    }

    @PostMapping("/chat")
    public String chatPost(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String sessionId,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        if ("NEW_SESSION".equalsIgnoreCase(action)) {
            String previous =
                    sessionId != null && !sessionId.isBlank()
                            ? sessionId
                            : SessionCookieSupport.readExisting(request).orElse(null);
            if (previous != null) {
                todoStateStore.clear(previous);
            }
            String newId = idGenerator.generateId();
            SessionCookieSupport.writeSessionCookie(response, newId);
            return "redirect:/chat";
        }
        if (message == null || message.isBlank()) {
            return "redirect:/chat";
        }
        String sid = sessionId != null && !sessionId.isBlank()
                ? sessionId
                : SessionCookieSupport.readOrCreate(request, response, idGenerator);
        ChatTurnResult turn = orchestrationService.exchange(sid, message);
        if (turn.status() == ChatTurnResult.Status.ASK_USER) {
            model.addAttribute("sessionId", sid);
            model.addAttribute("ticketId", turn.ticketId());
            model.addAttribute("prompt", turn.prompt());
            model.addAttribute("options", turn.options());
            model.addAttribute("profiles", String.join(",", environment.getActiveProfiles()));
            model.addAttribute("todos", todoStateStore.list(sid));
            return "chat_answer";
        }
        return "redirect:/chat?reply=" + java.net.URLEncoder.encode(turn.reply(), java.nio.charset.StandardCharsets.UTF_8);
    }

    @GetMapping("/chat/answer")
    public String chatAnswerGet(Model model, HttpServletRequest request, HttpServletResponse response) {
        String sid = SessionCookieSupport.readOrCreate(request, response, idGenerator);
        model.addAttribute("sessionId", sid);
        model.addAttribute("profiles", String.join(",", environment.getActiveProfiles()));
        model.addAttribute("todos", todoStateStore.list(sid));
        return "chat_answer";
    }

    @PostMapping("/chat/answer")
    public String chatAnswerPost(
            @RequestParam String ticketId,
            @RequestParam String sessionId,
            @RequestParam(required = false) List<String> selectedOptionIds,
            @RequestParam(required = false) String freeText,
            HttpServletRequest request,
            HttpServletResponse response) {
        SessionCookieSupport.readOrCreate(request, response, idGenerator);
        List<String> selected = selectedOptionIds == null ? List.of() : selectedOptionIds;
        ChatTurnResult turn = orchestrationService.resumeAnswer(sessionId, ticketId, selected, freeText);
        return "redirect:/chat?reply=" + java.net.URLEncoder.encode(turn.reply(), java.nio.charset.StandardCharsets.UTF_8);
    }

    @GetMapping("/evaluation")
    public String evaluationGet(Model model) {
        model.addAttribute("profiles", String.join(",", environment.getActiveProfiles()));
        return "evaluation";
    }

    @PostMapping("/evaluation/run")
    public String evaluationRun(
            @RequestParam String dataset,
            @RequestParam(required = false, defaultValue = "stub-ai") String profile,
            Model model)
            throws Exception {
        String report = evaluationService.run(dataset, profile);
        model.addAttribute("report", report);
        model.addAttribute("dataset", dataset);
        model.addAttribute("profile", profile);
        model.addAttribute("profiles", String.join(",", environment.getActiveProfiles()));
        return "evaluation_result";
    }
}
