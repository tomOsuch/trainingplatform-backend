package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.service.model.GoalProgress;

@Mapper(componentModel = "spring")
public interface GoalMapper {

    @Mapping(target = "id", source = "goal.id")
    @Mapping(target = "targetValue", source = "goal.targetValue")
    @Mapping(target = "categoryId", source = "goal.category.id")
    @Mapping(target = "categoryName", source = "goal.category.name")
    @Mapping(target = "categoryColor", source = "goal.category.color")
    @Mapping(target = "currentValue", source = "progress.currentValue")
    @Mapping(target = "percent", expression = "java(progress.percent())")
    @Mapping(target = "targetReached", expression = "java(progress.targetReached())")
    @Mapping(target = "achieved", source = "goal.achieved")
    GoalResponse toResponse(Goal goal, GoalProgress progress);
}
