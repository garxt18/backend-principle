package com.example.taskmanager.task.dto;

import com.example.taskmanager.task.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 5000) String description,
        TaskPriority priority,
        @FutureOrPresent LocalDate dueDate) {
}
