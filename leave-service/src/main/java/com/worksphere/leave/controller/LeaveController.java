package com.worksphere.leave.controller;

import com.worksphere.leave.dto.LeaveRejectRequestDto;
import com.worksphere.leave.dto.LeaveRequestDto;
import com.worksphere.leave.dto.LeaveResponseDto;
import com.worksphere.leave.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    public ResponseEntity<LeaveResponseDto> applyLeave(
            @Valid @RequestBody LeaveRequestDto request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(leaveService.applyLeave(request));
    }

    @GetMapping("/{leaveId}")
    public ResponseEntity<LeaveResponseDto> getLeaveById(
            @PathVariable Long leaveId) {

        return ResponseEntity.ok(
                leaveService.getLeaveById(leaveId)
        );
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveResponseDto>> getLeavesByEmployee(
            @PathVariable Long employeeId) {

        return ResponseEntity.ok(
                leaveService.getLeavesByEmployee(employeeId)
        );
    }

    @PutMapping("/{leaveId}/approve")
    public ResponseEntity<LeaveResponseDto> approveLeave(
            @PathVariable Long leaveId,
            @RequestParam Long approverId) {

        return ResponseEntity.ok(
                leaveService.approveLeave(
                        leaveId,
                        approverId
                )
        );
    }

    @PutMapping("/{leaveId}/reject")
    public ResponseEntity<LeaveResponseDto> rejectLeave(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveRejectRequestDto request) {

        return ResponseEntity.ok(
                leaveService.rejectLeave(
                        leaveId,
                        request
                )
        );
    }

    @PutMapping("/{leaveId}/cancel")
    public ResponseEntity<LeaveResponseDto> cancelLeave(
            @PathVariable Long leaveId) {

        return ResponseEntity.ok(
                leaveService.cancelLeave(leaveId)
        );
    }
}