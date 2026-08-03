package com.worksphere.payroll.mapper;

import com.worksphere.payroll.dto.request.CreatePayrollRequest;
import com.worksphere.payroll.dto.request.UpdatePayrollRequest;
import com.worksphere.payroll.dto.response.PayrollResponse;
import com.worksphere.payroll.entity.Payroll;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PayrollMapper {

    /**
     * Entity -> Response DTO
     */
    PayrollResponse toResponse(Payroll payroll);

    /**
     * Create Request -> Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "netSalary", ignore = true)
    Payroll toEntity(CreatePayrollRequest request);

    /**
     * Update existing Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "netSalary", ignore = true)
    void updateEntity(UpdatePayrollRequest request,
                      @MappingTarget Payroll payroll);
}