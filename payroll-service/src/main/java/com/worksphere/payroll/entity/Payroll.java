package com.worksphere.payroll.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "payroll",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payroll_employee_id",
                        columnNames = "employee_id"
                )
        }
)
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "employee_id",
            nullable = false
    )
    private Long employeeId;

    @Column(
            name = "basic_salary",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal basicSalary;

    @Column(
            name = "bonus",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal bonus;

    @Column(
            name = "tax",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal tax;

    @Column(
            name = "net_salary",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal netSalary;
}