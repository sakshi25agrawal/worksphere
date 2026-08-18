package com.worksphere.leave.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "leave_balances",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_leave_type_year",
                        columnNames = {
                                "employee_id",
                                "leave_type_id",
                                "year"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceEntity {

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
                    name = "fk_leave_balance_leave_type"
            )
    )
    private LeaveTypeEntity leaveType;

    @Column(
            name = "allocated_days",
            nullable = false
    )
    private Integer allocatedDays;

    @Column(
            name = "used_days",
            nullable = false
    )
    private Integer usedDays;

    @Column(
            name = "remaining_days",
            nullable = false
    )
    private Integer remainingDays;

    @Column(nullable = false)
    private Integer year;
}