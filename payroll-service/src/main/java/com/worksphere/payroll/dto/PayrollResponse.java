package com.worksphere.payroll.dto;

public class PayrollResponse {

    private Long id;

    private Long employeeId;

    private Double basicSalary;

    private Double bonus;

    private Double tax;

    private Double netSalary;

    public PayrollResponse() {
    }

    public PayrollResponse(Long id,
                           Long employeeId,
                           Double basicSalary,
                           Double bonus,
                           Double tax,
                           Double netSalary) {
        this.id = id;
        this.employeeId = employeeId;
        this.basicSalary = basicSalary;
        this.bonus = bonus;
        this.tax = tax;
        this.netSalary = netSalary;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(Double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public Double getBonus() {
        return bonus;
    }

    public void setBonus(Double bonus) {
        this.bonus = bonus;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(Double netSalary) {
        this.netSalary = netSalary;
    }
}