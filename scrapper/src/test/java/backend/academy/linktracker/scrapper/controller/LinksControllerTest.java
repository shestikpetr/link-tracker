package backend.academy.linktracker.scrapper.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.LinkNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.service.LinkService;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LinksControllerTest {

    LinkService linkService = mock(LinkService.class);
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LinksController(linkService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void get_links_returns_list() throws Exception {
        var link = new LinkResponse(1L, URI.create("https://github.com/foo/bar"), List.of("tag"), List.of());
        when(linkService.getLinks(eq(1L), any())).thenReturn(List.of(link));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("https://github.com/foo/bar"))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void get_links_with_tags_passes_tags_to_service() throws Exception {
        when(linkService.getLinks(eq(1L), any())).thenReturn(List.of());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1).param("tags", "work", "hobby"))
                .andExpect(status().isOk());
    }

    @Test
    void add_link_returns_200_with_response() throws Exception {
        var response = new LinkResponse(1L, URI.create("https://github.com/foo/bar"), List.of(), List.of());
        when(linkService.addLink(anyLong(), any(), anyList(), anyList())).thenReturn(response);

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://github.com/foo/bar", "tags": [], "filters": []}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void add_link_to_nonexistent_chat_returns_404() throws Exception {
        doThrow(new ChatNotFoundException(2L)).when(linkService).addLink(anyLong(), any(), anyList(), anyList());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://github.com/foo/bar", "tags": [], "filters": []}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void add_link_already_tracked_returns_409() throws Exception {
        doThrow(new LinkAlreadyExistsException(URI.create("https://github.com/foo/bar")))
                .when(linkService)
                .addLink(anyLong(), any(), anyList(), anyList());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://github.com/foo/bar", "tags": [], "filters": []}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void add_unsupported_link_returns_422() throws Exception {
        doThrow(new UnsupportedLinkException(URI.create("https://unsupported.com/foo")))
                .when(linkService)
                .addLink(anyLong(), any(), anyList(), anyList());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://unsupported.com/foo", "tags": [], "filters": []}
                                """))
                .andExpect(status().is(422));
    }

    @Test
    void update_link_returns_200_with_response() throws Exception {
        var response =
                new LinkResponse(1L, URI.create("https://github.com/foo/bar"), List.of("new-tag"), List.of("flt"));
        when(linkService.updateLink(anyLong(), any(), anyList(), anyList())).thenReturn(response);

        mockMvc.perform(put("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://github.com/foo/bar", "tags": ["new-tag"], "filters": ["flt"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value("new-tag"))
                .andExpect(jsonPath("$.filters[0]").value("flt"));
    }

    @Test
    void update_nonexistent_link_returns_404() throws Exception {
        doThrow(new LinkNotFoundException(URI.create("https://github.com/foo/bar")))
                .when(linkService)
                .updateLink(anyLong(), any(), anyList(), anyList());

        mockMvc.perform(put("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://github.com/foo/bar", "tags": [], "filters": []}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_link_for_nonexistent_chat_returns_404() throws Exception {
        doThrow(new ChatNotFoundException(999L)).when(linkService).updateLink(anyLong(), any(), anyList(), anyList());

        mockMvc.perform(put("/links")
                        .header("Tg-Chat-Id", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://github.com/foo/bar", "tags": [], "filters": []}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void remove_link_returns_200() throws Exception {
        var response = new LinkResponse(1L, URI.create("https://github.com/foo/bar"), List.of(), List.of());
        when(linkService.removeLink(anyLong(), any())).thenReturn(response);

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://github.com/foo/bar"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void remove_nonexistent_link_returns_404() throws Exception {
        doThrow(new LinkNotFoundException(URI.create("https://github.com/foo/bar")))
                .when(linkService)
                .removeLink(anyLong(), any());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://github.com/foo/bar"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void remove_link_from_nonexistent_chat_returns_404() throws Exception {
        doThrow(new ChatNotFoundException(999L)).when(linkService).removeLink(eq(999L), any());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link": "https://github.com/foo/bar"}
                                """))
                .andExpect(status().isNotFound());
    }
}
