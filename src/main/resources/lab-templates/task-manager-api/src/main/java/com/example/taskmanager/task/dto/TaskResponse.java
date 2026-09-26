package com.example.taskmanager.task.dto;

import com.example.taskmanager.task.Task;
import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                task.getPriority(), task.getDueDate(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
