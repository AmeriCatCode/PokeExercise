package com.example.taskapi.task;

import org.springframework.stereotype.Component;

import com.example.taskapi.task.dto.TaskRequest;
import com.example.taskapi.task.dto.TaskResponse;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate());
    }

    /** Copies the editable fields from the request onto the entity. Never touches id or owner. */
    public void apply(TaskRequest request, Task task) {
        task.setTitle(request.title().strip());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());
    }
}
