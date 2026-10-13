package com.example.taskapi.task.dto;

import java.time.LocalDate;

import com.example.taskapi.task.TaskStatus;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate) {
}
