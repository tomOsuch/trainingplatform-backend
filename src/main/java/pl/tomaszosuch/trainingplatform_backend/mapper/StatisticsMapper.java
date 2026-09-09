package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CategoryStatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PlanCompletionResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.StatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.service.model.CategoryStatistics;
import pl.tomaszosuch.trainingplatform_backend.service.model.PlanCompletion;
import pl.tomaszosuch.trainingplatform_backend.service.model.WorkoutStatistics;

import java.time.LocalDate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StatisticsMapper {

    @Mapping(target = "workoutCount", source = "statistics.totalSessions")
    @Mapping(target = "totalMinutes", source = "statistics.totalMinutes")
    @Mapping(target = "byCategory", source = "statistics.byCategory")
    @Mapping(target = "planCompletion", source = "completion")
    StatisticsResponse toResponse(LocalDate from, LocalDate to,
                                  WorkoutStatistics statistics, PlanCompletion completion);

    @Mapping(target = "workoutCount", source = "sessions")
    @Mapping(target = "totalMinutes", source = "minutes")
    CategoryStatisticsResponse toCategoryResponse(CategoryStatistics category);

    @Mapping(target = "completionBase", expression = "java(completion.completionBase())")
    @Mapping(target = "completionRate", expression = "java(completion.completionRate())")
    PlanCompletionResponse toPlanCompletionResponse(PlanCompletion completion);
}
