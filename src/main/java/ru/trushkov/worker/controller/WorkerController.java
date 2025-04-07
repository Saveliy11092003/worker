package ru.trushkov.worker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.trushkov.worker.service.WorkerService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/api/worker")
public class WorkerController {
    private final WorkerService workerService;

    @GetMapping("/health")
    public String healthCheck() {
        return "OK";
    }

    @PostMapping("/getIndex")
    public Integer getCurrentIndex(@RequestBody String requestId) {
        return workerService.getCurrentIndex(requestId);
    }
}
