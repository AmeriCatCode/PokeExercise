package com.example.taskapi.task;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskapi.exception.TaskNotFoundException;
import com.example.taskapi.exception.UnknownUserException;
import com.example.taskapi.task.dto.TaskRequest;
import com.example.taskapi.task.dto.TaskResponse;
import com.example.taskapi.user.User;
import com.example.taskapi.user.UserRepository;

/**
 * Task use cases. Every method takes the username of the authenticated caller
 * (taken from the JWT by the controller) and only ever touches that user's tasks.
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    public TaskResponse create(String username, TaskRequest request) {
        Task task = new Task(currentUser(username));
        taskMapper.apply(request, task);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(String username) {
        return taskRepository.findAllByUserIdOrderByIdAsc(currentUser(username).getId()).stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(String username, Long id) {
        return taskMapper.toResponse(ownedTask(username, id));
    }

    public TaskResponse update(String username, Long id, TaskRequest request) {
        Task task = ownedTask(username, id);
        taskMapper.apply(request, task);
        return taskMapper.toResponse(task);
    }

    public void delete(String username, Long id) {
        taskRepository.delete(ownedTask(username, id));
    }

    private Task ownedTask(String username, Long id) {
        return taskRepository.findByIdAndUserId(id, currentUser(username).getId())
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    private User currentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(UnknownUserException::new);
    }
}
