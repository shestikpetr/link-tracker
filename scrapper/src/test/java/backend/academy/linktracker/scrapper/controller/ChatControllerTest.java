package backend.academy.linktracker.scrapper.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatControllerTest {

    ChatService chatService = mock(ChatService.class);
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_new_chat_returns_200() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        verify(chatService).register(1L);
    }

    @Test
    void register_existing_chat_returns_409() throws Exception {
        doThrow(new ChatAlreadyExistsException(1L)).when(chatService).register(1L);

        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isConflict());
    }

    @Test
    void delete_existing_chat_returns_200() throws Exception {
        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isOk());

        verify(chatService).delete(1L);
    }

    @Test
    void delete_nonexistent_chat_returns_404() throws Exception {
        doThrow(new ChatNotFoundException(1L)).when(chatService).delete(1L);

        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isNotFound());
    }
}
