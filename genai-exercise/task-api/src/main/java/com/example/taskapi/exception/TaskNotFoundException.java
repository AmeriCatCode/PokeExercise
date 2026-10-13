package com.example.taskapi.exception;

/** Thrown when a task does not exist or is not owned by the caller; both cases map to 404. */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("Task " + id + " not found");
    }
}
