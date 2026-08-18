package com.worksphere.leave.entity;

import com.worksphere.leave.enums.LeaveStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "leave_requests",
        indexes = {
                @Index(
                        name = "idx_leave_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_leave_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_leave_dates",
                        columnList = "start_date,end_date"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "employee_id",
            nullable = false
    )
    private Long employeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "leave_type_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_leave_request_leave_type"
            )
    )
    private LeaveTypeEntity leaveType;

    @Column(
            name = "start_date",
            nullable = false
    )
    private LocalDate startDate;

    @Column(
            name = "end_date",
            nullable = false
    )
    private LocalDate endDate;

    @Column(
            name = "number_of_days",
            nullable = false
    )
    private Integer numberOfDays;

    @Column(
            nullable = false,
            length = 500
    )
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private LeaveStatus status;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "approver_id")
    private Long approverId;

    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;
}