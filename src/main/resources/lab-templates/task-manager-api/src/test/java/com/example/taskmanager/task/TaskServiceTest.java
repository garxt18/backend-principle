package com.example.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskmanager.common.ResourceNotFoundException;
import com.example.taskmanager.task.dto.CreateTaskRequest;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.task.dto.UpdateTaskRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createDefaultsPriorityToMedium() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(new CreateTaskRequest("Learn JPA", null, null, null));

        assertThat(response.title()).isEqualTo("Learn JPA");
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        Task task = new Task("Old title", "keep me", TaskPriority.LOW, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.update(1L,
                new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null));

        assertThat(response.title()).isEqualTo("Old title");
        assertThat(response.description()).isEqualTo("keep me");
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void getUnknownTaskThrowsNotFound() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.get(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }
}
