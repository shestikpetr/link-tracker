package backend.academy.linktracker.bot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.bot.service.LinkUpdateNotifier;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

class LinkUpdateControllerTest {

    LinkUpdateNotifier linkUpdateNotifier = mock(LinkUpdateNotifier.class);
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = new SpringValidatorAdapter(factory.getValidator());
            mockMvc = MockMvcBuilders.standaloneSetup(new LinkUpdateController(linkUpdateNotifier))
                    .setValidator(validator)
                    .build();
        }
    }

    @Test
    void valid_update_body_returns_200() throws Exception {
        mockMvc.perform(post("/updates").contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "id": 1,
                              "url": "https://github.com/foo/bar",
                              "description": "обновление",
                              "tgChatIds": [123, 456]
                            }
                            """))
                .andExpect(status().isOk());

        verify(linkUpdateNotifier).notify(any());
    }

    @Test
    void missing_required_fields_returns_400() throws Exception {
        mockMvc.perform(post("/updates").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformed_json_returns_400() throws Exception {
        mockMvc.perform(post("/updates").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
    }
}
