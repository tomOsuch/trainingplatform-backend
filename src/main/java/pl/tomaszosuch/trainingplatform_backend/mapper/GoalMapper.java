package pl.tomaszosuch.trainingplatform_backend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalDetailsResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalLogEntryResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.service.model.GoalProgress;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
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

    // Te same odwzorowania co wyżej plus entries — wpisy mapuje toLogEntry, element po elemencie.
    @Mapping(target = "id", source = "goal.id")
    @Mapping(target = "targetValue", source = "goal.targetValue")
    @Mapping(target = "categoryId", source = "goal.category.id")
    @Mapping(target = "categoryName", source = "goal.category.name")
    @Mapping(target = "categoryColor", source = "goal.category.color")
    @Mapping(target = "currentValue", source = "progress.currentValue")
    @Mapping(target = "percent", expression = "java(progress.percent())")
    @Mapping(target = "targetReached", expression = "java(progress.targetReached())")
    @Mapping(target = "achieved", source = "goal.achieved")
    @Mapping(target = "entries", source = "logs")
    GoalDetailsResponse toDetailsResponse(Goal goal, GoalProgress progress, List<WorkoutLog> logs);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryColor", source = "category.color")
    GoalLogEntryResponse toLogEntry(WorkoutLog log);

}