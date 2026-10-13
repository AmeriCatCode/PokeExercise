package com.example.taskapi.task;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.taskapi.task.dto.TaskRequest;
import com.example.taskapi.task.dto.TaskResponse;

import jakarta.validation.Valid;

/** The caller is always the JWT subject; ownership is never read from the request. */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody TaskRequest request) {
        TaskResponse created = taskService.create(jwt.getSubject(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<TaskResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return taskService.findAll(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public TaskResponse findById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return taskService.findById(jwt.getSubject(), id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal Jwt jwt,
                               @PathVariable Long id,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.update(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        taskService.delete(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
