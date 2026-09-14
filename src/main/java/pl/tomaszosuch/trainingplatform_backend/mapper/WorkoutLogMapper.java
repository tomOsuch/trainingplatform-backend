package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutLogResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkoutLogMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "category.color", target = "categoryColor")
    @Mapping(source = "category.iconName", target = "categoryIconName")
    @Mapping(source = "plan.id", target = "planId")
    WorkoutLogResponse toResponse(WorkoutLog log);

}
