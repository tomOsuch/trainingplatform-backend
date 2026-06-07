package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;

import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutCategoryResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;

@Mapper(componentModel = "spring")
public interface WorkoutCategoryMapper {
    WorkoutCategoryResponse toResponse(WorkoutCategory category);
}
