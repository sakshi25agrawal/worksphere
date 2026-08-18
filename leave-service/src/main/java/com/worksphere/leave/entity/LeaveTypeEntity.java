package com.worksphere.leave.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "leave_types",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_leave_type_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 50
    )
    private String code;

    @Column(
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            name = "annual_allocation",
            nullable = false
    )
    private Integer annualAllocation;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Boolean active;
}