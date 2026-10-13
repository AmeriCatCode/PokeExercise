package com.example.taskapi.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Scoped lookup: empty both when the task does not exist and when it belongs to someone else. */
    Optional<Task> findByIdAndUserId(Long id, Long userId);

    List<Task> findAllByUserIdOrderByIdAsc(Long userId);
}
