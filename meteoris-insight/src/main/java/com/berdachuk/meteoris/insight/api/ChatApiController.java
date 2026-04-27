package com.berdachuk.meteoris.insight.api;

import com.berdachuk.meteoris.insight.agent.api.ChatTurnResult;
import com.berdachuk.meteoris.insight.agent.api.ChatOrchestration;
import com.berdachuk.meteoris.insight.api.generated.ChatApi;
import com.berdachuk.meteoris.insight.api.generated.model.AskUserOption;
import com.berdachuk.meteoris.insight.api.generated.model.ChatExchangeResponse;
import com.berdachuk.meteoris.insight.api.generated.model.ChatMessageRequest;
import com.berdachuk.meteoris.insight.api.generated.model.NewSessionResponse;
import com.berdachuk.meteoris.insight.api.generated.model.QuestionAnswerRequest;
import com.berdachuk.meteoris.insight.core.IdGenerator;
import com.berdachuk.meteoris.insight.memory.TodoStateStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class ChatApiController implements ChatApi {

    private final ChatOrchestration orchestrationService;
    private final IdGenerator idGenerator;
    private final TodoStateStore todoStateStore;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public ChatApiController(
            ChatOrchestration orchestrationService,
            IdGenerator idGenerator,
            TodoStateStore todoStateStore,
            HttpServletRequest request,
            HttpServletResponse response) {
        this.orchestrationService = orchestrationService;
        this.idGenerator = idGenerator;
        this.todoStateStore = todoStateStore;
        this.request = request;
        this.response = response;
    }

    @Override
    public ResponseEntity<NewSessionResponse> postNewChatSession() {
        SessionCookieSupport.readExisting(request).ifPresent(todoStateStore::clear);
        String newId = idGenerator.generateId();
        SessionCookieSupport.writeSessionCookie(response, newId);
        return ResponseEntity.ok(new NewSessionResponse(newId));
    }

    @Override
    public ResponseEntity<ChatExchangeResponse> postChatMessage(ChatMessageRequest chatMessageRequest) {
        String raw = chatMessageRequest.getMessage();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank");
        }
        String sessionId = chatMessageRequest.getSessionId() != null && !chatMessageRequest.getSessionId().isBlank()
                ? chatMessageRequest.getSessionId()
                : SessionCookieSupport.readOrCreate(request, response, idGenerator);
        ChatTurnResult turn = orchestrationService.exchange(sessionId, raw);
        return ResponseEntity.ok(toApi(turn));
    }

    @Override
    public ResponseEntity<ChatExchangeResponse> postQuestionAnswer(
            String ticketId, QuestionAnswerRequest questionAnswerRequest) {
        String sessionId = questionAnswerRequest.getSessionId() != null
                        && !questionAnswerRequest.getSessionId().isBlank()
                ? questionAnswerRequest.getSessionId()
                : SessionCookieSupport.readOrCreate(request, response, idGenerator);
        List<String> selected =
                questionAnswerRequest.getSelectedOptionIds() == null
                        ? List.of()
                        : questionAnswerRequest.getSelectedOptionIds();
        ChatTurnResult turn =
                orchestrationService.resumeAnswer(sessionId, ticketId, selected, questionAnswerRequest.getFreeText());
        return ResponseEntity.ok(toApi(turn));
    }

    private static ChatExchangeResponse toApi(ChatTurnResult t) {
        ChatExchangeResponse r = new ChatExchangeResponse(
                t.sessionId(), ChatExchangeResponse.StatusEnum.fromValue(t.status().name()));
        r.setReply(t.reply());
        r.setModelName(t.modelName());
        r.setTicketId(t.ticketId());
        r.setPrompt(t.prompt());
        if (t.options() != null && !t.options().isEmpty()) {
            List<AskUserOption> opts = new ArrayList<>();
            for (ChatTurnResult.AskUserOptionView o : t.options()) {
                AskUserOption ao = new AskUserOption();
                ao.setId(o.id());
                ao.setLabel(o.label());
                opts.add(ao);
            }
            r.setOptions(opts);
        }
        return r;
    }
}
