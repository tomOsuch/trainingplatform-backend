package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.mapstruct.ReportingPolicy;
import pl.tomaszosuch.trainingplatform_backend.dto.response.TrainingPlanResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TrainingPlanMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryColor", source = "category.color")
    @Mapping(target = "categoryIconName", source = "category.iconName")
    TrainingPlanResponse toResponse(TrainingPlan trainingPlan);

}
