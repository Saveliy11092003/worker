package ru.trushkov.worker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;
import ru.trushkov.worker.service.WorkerService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/api/worker")
public class WorkerController {
    private final WorkerService workerService;

    @PostMapping("/hash/crack/task")
    public CrackHashManagerRequest crackPassword(@RequestBody CrackHashManagerRequest crackHashManagerRequest) {
        workerService.task(crackHashManagerRequest);
        return crackHashManagerRequest;
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "OK";
    }

    @PostMapping("/getIndex")
    public Integer getCurrentIndex(@RequestBody String requestId) {
        return workerService.getCurrentIndex(requestId);
    }
}
