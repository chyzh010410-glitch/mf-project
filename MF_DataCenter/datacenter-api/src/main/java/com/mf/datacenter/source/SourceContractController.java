package com.mf.datacenter.source;

import com.mf.datacenter.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/source")
public class SourceContractController {

    private final SourceContractService sourceContractService;

    public SourceContractController(SourceContractService sourceContractService) {
        this.sourceContractService = sourceContractService;
    }

    @GetMapping("/contracts")
    public ApiResponse<List<SourceContractService.SourceTableContract>> contracts() {
        return ApiResponse.ok(sourceContractService.contracts());
    }

    @GetMapping("/check")
    public ApiResponse<SourceContractService.SourceCheckSummary> check() {
        return ApiResponse.ok(sourceContractService.check());
    }

    @PostMapping("/check")
    public ApiResponse<SourceContractService.SourceCheckSummary> runCheck() {
        return ApiResponse.ok(sourceContractService.check());
    }
}
