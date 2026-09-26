package com.example.taskmanager.task.dto;

import com.example.taskmanager.task.TaskPriority;
import com.example.taskmanager.task.TaskStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** PATCH body: every field is optional - only the fields that are present get changed. */
public record UpdateTaskRequest(
        @Size(min = 1, max = 120) String title,
        @Size(max = 5000) String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate) {
}
