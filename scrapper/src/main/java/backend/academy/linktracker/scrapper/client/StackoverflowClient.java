package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.StackoverflowAnswerResponse;
import backend.academy.linktracker.scrapper.dto.StackoverflowCommentResponse;
import backend.academy.linktracker.scrapper.dto.StackoverflowQuestionResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(accept = "application/json")
public interface StackoverflowClient {
    @GetExchange("/2.3/questions/{id}?site=stackoverflow")
    StackoverflowQuestionResponse getQuestions(@PathVariable Long id);

    @GetExchange("/2.3/questions/{id}/answers")
    StackoverflowAnswerResponse getAnswers(
            @PathVariable Long id,
            @RequestParam String order,
            @RequestParam String sort,
            @RequestParam String site,
            @RequestParam String filter);

    @GetExchange("/2.3/questions/{id}/comments")
    StackoverflowCommentResponse getComments(
            @PathVariable Long id,
            @RequestParam String order,
            @RequestParam String sort,
            @RequestParam String site,
            @RequestParam String filter);
}
