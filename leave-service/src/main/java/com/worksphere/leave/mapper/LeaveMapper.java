package com.worksphere.leave.mapper;

import com.worksphere.leave.dto.LeaveBalanceResponseDto;
import com.worksphere.leave.dto.LeaveRequestDto;
import com.worksphere.leave.dto.LeaveResponseDto;
import com.worksphere.leave.dto.LeaveTypeResponseDto;
import com.worksphere.leave.entity.LeaveBalanceEntity;
import com.worksphere.leave.entity.LeaveRequestEntity;
import com.worksphere.leave.entity.LeaveTypeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LeaveMapper {


    LeaveRequestEntity toEntity(LeaveRequestDto dto);

    @Mapping(target = "leaveTypeId", source = "leaveType.id")
    @Mapping(target = "leaveTypeCode", source = "leaveType.code")
    @Mapping(target = "leaveTypeName", source = "leaveType.name")
    LeaveResponseDto toResponseDto(LeaveRequestEntity entity);

    @Mapping(target = "leaveTypeId", source = "leaveType.id")
    @Mapping(target = "leaveTypeCode", source = "leaveType.code")
    @Mapping(target = "leaveTypeName", source = "leaveType.name")
    LeaveBalanceResponseDto toBalanceResponseDto(
            LeaveBalanceEntity entity
    );

    LeaveTypeResponseDto toLeaveTypeResponseDto(
            LeaveTypeEntity entity
    );
}