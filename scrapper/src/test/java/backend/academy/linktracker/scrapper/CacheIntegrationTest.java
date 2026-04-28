package backend.academy.linktracker.scrapper;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CacheIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @Autowired
    CacheManager cacheManager;

    @MockitoSpyBean
    LinkRepository linkRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        cacheManager.getCache("links").clear();
    }

    @Test
    void second_get_links_is_served_from_cache() throws Exception {
        long chatId = 201L;
        registerChatWithLink(chatId, "https://github.com/foo/bar201");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", chatId)).andExpect(status().isOk());
        mockMvc.perform(get("/links").header("Tg-Chat-Id", chatId)).andExpect(status().isOk());

        verify(linkRepository, times(1)).findByChat(chatId);
    }

    @Test
    void cache_is_invalidated_after_add_link() throws Exception {
        long chatId = 202L;
        mockMvc.perform(post("/tg-chat/" + chatId)).andExpect(status().isOk());
        addLink(chatId, "https://github.com/foo/bar202a");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", chatId)).andExpect(status().isOk());
        addLink(chatId, "https://github.com/foo/bar202b");
        mockMvc.perform(get("/links").header("Tg-Chat-Id", chatId)).andExpect(status().isOk());

        verify(linkRepository, times(2)).findByChat(chatId);
    }

    @Test
    void cache_is_invalidated_after_remove_link() throws Exception {
        long chatId = 203L;
        registerChatWithLink(chatId, "https://github.com/foo/bar203");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", chatId)).andExpect(status().isOk());
        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"link":"https://github.com/foo/bar203"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/links").header("Tg-Chat-Id", chatId)).andExpect(status().isOk());

        verify(linkRepository, times(2)).findByChat(chatId);
    }

    @Test
    void cache_entry_expires_after_ttl() throws Exception {
        long chatId = 204L;
        registerChatWithLink(chatId, "https://github.com/foo/bar204");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", chatId)).andExpect(status().isOk());

        Awaitility.await()
                .pollDelay(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    mockMvc.perform(get("/links").header("Tg-Chat-Id", chatId)).andExpect(status().isOk());
                    verify(linkRepository, times(2)).findByChat(chatId);
                });
    }

    private void registerChatWithLink(long chatId, String url) throws Exception {
        mockMvc.perform(post("/tg-chat/" + chatId)).andExpect(status().isOk());
        addLink(chatId, url);
    }

    private void addLink(long chatId, String url) throws Exception {
        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"" + url + "\",\"tags\":[],\"filters\":[]}"))
                .andExpect(status().isOk());
    }
}
