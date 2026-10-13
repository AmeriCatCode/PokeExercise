package com.example.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.taskapi.exception.TaskNotFoundException;
import com.example.taskapi.exception.UnknownUserException;
import com.example.taskapi.task.dto.TaskRequest;
import com.example.taskapi.task.dto.TaskResponse;
import com.example.taskapi.user.User;
import com.example.taskapi.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    private TaskService taskService;
    private User alice;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository, new TaskMapper());
        alice = new User("alice");
        ReflectionTestUtils.setField(alice, "id", 1L);
    }

    @Test
    void createAssignsTheAuthenticatedUserAsOwner() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create("alice",
                new TaskRequest("  Write report  ", "Q3 numbers", TaskStatus.TODO, LocalDate.of(2026, 11, 1)));

        ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(alice);
        assertThat(response.title()).isEqualTo("Write report");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void findByIdOfTaskNotOwnedByCallerThrowsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById("alice", 99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updateChangesEditableFields() {
        Task task = existingTask();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.update("alice", 10L,
                new TaskRequest("New title", null, TaskStatus.DONE, null));

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getUser()).isSameAs(alice);
    }

    @Test
    void deleteRemovesOwnedTask() {
        Task task = existingTask();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(task));

        taskService.delete("alice", 10L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteOfMissingTaskThrowsNotFoundAndDeletesNothing() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(taskRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete("alice", 10L))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void unknownJwtSubjectIsRejected() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findAll("ghost"))
                .isInstanceOf(UnknownUserException.class);
    }

    private Task existingTask() {
        Task task = new Task(alice);
        ReflectionTestUtils.setField(task, "id", 10L);
        task.setTitle("Old title");
        task.setStatus(TaskStatus.TODO);
        return task;
    }
}
