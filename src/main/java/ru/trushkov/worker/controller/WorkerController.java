package ru.trushkov.worker.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

}
