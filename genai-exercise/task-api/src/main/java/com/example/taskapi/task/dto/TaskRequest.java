package com.example.taskapi.task.dto;

import java.time.LocalDate;

import com.example.taskapi.task.Task;
import com.example.taskapi.task.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for create (POST) and full update (PUT). Deliberately has no owner field:
 * ownership always comes from the authenticated JWT user.
 */
public record TaskRequest(
        @NotBlank @Size(max = Task.TITLE_MAX_LENGTH) String title,
        @Size(max = Task.DESCRIPTION_MAX_LENGTH) String description,
        @NotNull TaskStatus status,
        LocalDate dueDate) {
}
