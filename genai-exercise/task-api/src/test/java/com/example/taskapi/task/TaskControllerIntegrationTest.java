package com.example.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.taskapi.user.User;
import com.example.taskapi.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    private static final String VALID_BODY = """
            {"title": "Write report", "description": "Q3 numbers", "status": "TODO", "dueDate": "2026-11-01"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        alice = userRepository.save(new User("alice"));
        bob = userRepository.save(new User("bob"));
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        mockMvc.perform(json(post("/api/tasks"), VALID_BODY).with(as("alice")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/tasks/" + onlyTaskId())))
                .andExpect(jsonPath("$.title").value("Write report"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value("2026-11-01"))
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void createWithInvalidFieldsReturns400ProblemDetail() throws Exception {
        String body = """
                {"title": "  ", "status": null}
                """;
        mockMvc.perform(json(post("/api/tasks"), body).with(as("alice")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    void createWithTitleOver150CharsReturns400() throws Exception {
        String body = """
                {"title": "%s", "status": "TODO"}
                """.formatted("x".repeat(151));
        mockMvc.perform(json(post("/api/tasks"), body).with(as("alice")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void malformedJsonAndUnknownEnumReturn400() throws Exception {
        mockMvc.perform(json(post("/api/tasks"), "{not json").with(as("alice")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
        mockMvc.perform(json(post("/api/tasks"), """
                        {"title": "x", "status": "ARCHIVED"}
                        """).with(as("alice")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownershipFieldsInBodyAreRejected() throws Exception {
        String body = """
                {"title": "Sneaky", "status": "TODO", "userId": %d}
                """.formatted(bob.getId());
        mockMvc.perform(json(post("/api/tasks"), body).with(as("alice")))
                .andExpect(status().isBadRequest());
        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void ownerCanReadTheirTaskAndListOnlyTheirOwn() throws Exception {
        Task mine = saveTask(alice, "Mine");
        saveTask(bob, "Bob's");

        mockMvc.perform(get("/api/tasks/{id}", mine.getId()).with(as("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mine.getId()))
                .andExpect(jsonPath("$.title").value("Mine"));

        mockMvc.perform(get("/api/tasks").with(as("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Mine"));
    }

    @Test
    void anotherUsersTaskLooksExactlyLikeAMissingOne() throws Exception {
        Task bobsTask = saveTask(bob, "Bob's");

        mockMvc.perform(get("/api/tasks/{id}", bobsTask.getId()).with(as("alice")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(json(put("/api/tasks/{id}", bobsTask.getId()), VALID_BODY).with(as("alice")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/tasks/{id}", bobsTask.getId()).with(as("alice")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tasks/{id}", 999_999).with(as("alice")))
                .andExpect(status().isNotFound());

        Task unchanged = taskRepository.findById(bobsTask.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Bob's");
    }

    @Test
    void updateReturns200WithNewValues() throws Exception {
        Task task = saveTask(alice, "Old");
        String body = """
                {"title": "New", "description": "updated", "status": "IN_PROGRESS", "dueDate": "2026-12-24"}
                """;

        mockMvc.perform(json(put("/api/tasks/{id}", task.getId()), body).with(as("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.dueDate").value("2026-12-24"));

        mockMvc.perform(get("/api/tasks/{id}", task.getId()).with(as("alice")))
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void deleteReturns204AndTaskIsGone() throws Exception {
        Task task = saveTask(alice, "Temp");

        mockMvc.perform(delete("/api/tasks/{id}", task.getId()).with(as("alice")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tasks/{id}", task.getId()).with(as("alice")))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks/abc").with(as("alice")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenForUnknownUserReturns401() throws Exception {
        mockMvc.perform(get("/api/tasks").with(as("ghost")))
                .andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor as(String username) {
        return jwt().jwt(token -> token.subject(username));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private Task saveTask(User owner, String title) {
        Task task = new Task(owner);
        task.setTitle(title);
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDate.of(2026, 11, 1));
        return taskRepository.save(task);
    }

    private Long onlyTaskId() {
        return taskRepository.findAll().getFirst().getId();
    }
}
