package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pl.tomaszosuch.trainingplatform_backend.dto.response.TrainingPlanResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;

@Mapper(componentModel = "spring")
public interface TrainingPlanMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryColor", source = "category.color")
    TrainingPlanResponse toResponse(TrainingPlan trainingPlan);

}
