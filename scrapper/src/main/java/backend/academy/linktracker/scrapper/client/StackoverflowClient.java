package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.StackoverflowQuestionResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(accept = "application/json")
public interface StackoverflowClient {
    @GetExchange("/2.3/questions/{id}?site=stackoverflow&key={key}&access_token={access_token}")
    StackoverflowQuestionResponse getQuestions(@PathVariable Long id);
}
