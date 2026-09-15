package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutTemplateResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutTemplate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkoutTemplateMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryColor", source = "category.color")
    @Mapping(target = "categoryIconName", source = "category.iconName")
    WorkoutTemplateResponse toResponse(WorkoutTemplate template);
}
