package com.changan.multimodal.task.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.task.dto.CreateTaskRequest;
import com.changan.multimodal.task.dto.TaskControlRequest;
import com.changan.multimodal.task.dto.TaskInstance;
import com.changan.multimodal.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ApiResponse<TaskInstance> create(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.create(request));
    }

    @GetMapping
    public ApiResponse<List<TaskInstance>> list() {
        return ApiResponse.success(taskService.list());
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskInstance> detail(@PathVariable String taskId) {
        return ApiResponse.success(taskService.get(taskId));
    }

    @PostMapping("/{taskId}/start")
    public ApiResponse<TaskInstance> start(@PathVariable String taskId) {
        return ApiResponse.success(taskService.start(taskId));
    }

    @PostMapping("/{taskId}/control")
    public ApiResponse<TaskInstance> control(@PathVariable String taskId,
                                             @Valid @RequestBody TaskControlRequest request) {
        return ApiResponse.success(taskService.control(taskId, request));
    }
}
