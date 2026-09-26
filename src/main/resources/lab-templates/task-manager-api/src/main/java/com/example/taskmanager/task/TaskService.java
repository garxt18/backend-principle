package com.example.taskmanager.task;

import com.example.taskmanager.common.ResourceNotFoundException;
import com.example.taskmanager.task.dto.CreateTaskRequest;
import com.example.taskmanager.task.dto.TaskResponse;
import com.example.taskmanager.task.dto.UpdateTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Task task = new Task(request.title(), request.description(), request.priority(), request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(TaskStatus status, String search, Pageable pageable) {
        Page<Task> page;
        if (status != null) {
            page = taskRepository.findByStatus(status, pageable);
        } else if (search != null && !search.isBlank()) {
            page = taskRepository.findByTitleContainingIgnoreCase(search, pageable);
        } else {
            page = taskRepository.findAll(pageable);
        }
        return page.map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(findTask(id));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = findTask(id);
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        Task task = findTask(id);
        taskRepository.delete(task);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }
}
