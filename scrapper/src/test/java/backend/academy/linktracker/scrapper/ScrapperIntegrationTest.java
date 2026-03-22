package backend.academy.linktracker.scrapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ScrapperIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void scenario_3_1_add_and_get_link() throws Exception {
        mockMvc.perform(post("/tg-chat/101")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 101)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link":"https://github.com/foo/bar101","tags":[],"filters":[]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 101))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("https://github.com/foo/bar101"));
    }

    @Test
    void scenario_3_2_add_and_delete_link() throws Exception {
        mockMvc.perform(post("/tg-chat/102")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 102)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link":"https://github.com/foo/bar102","tags":[],"filters":[]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 102)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link":"https://github.com/foo/bar102"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 102))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void scenario_3_3_delete_from_nonexistent_chat_returns_not_ok() throws Exception {
        mockMvc.perform(post("/tg-chat/103")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 103)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link":"https://github.com/foo/bar103","tags":[],"filters":[]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link":"https://github.com/foo/bar103"}
                                """))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 103))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("https://github.com/foo/bar103"));
    }

    @Test
    void scenario_3_4_add_to_nonexistent_chat_returns_not_ok() throws Exception {
        mockMvc.perform(post("/tg-chat/104")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1042)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link":"https://github.com/foo/bar104","tags":[],"filters":[]}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void scenario_3_5_add_to_deleted_chat_returns_not_ok() throws Exception {
        mockMvc.perform(post("/tg-chat/105")).andExpect(status().isOk());

        mockMvc.perform(delete("/tg-chat/105")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 105)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link":"https://github.com/foo/bar105","tags":[],"filters":[]}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void scenario_3_6_delete_nonexistent_chat_returns_404() throws Exception {
        mockMvc.perform(delete("/tg-chat/106")).andExpect(status().isNotFound());
    }
}
