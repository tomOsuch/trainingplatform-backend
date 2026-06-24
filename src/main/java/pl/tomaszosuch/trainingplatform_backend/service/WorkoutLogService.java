package pl.tomaszosuch.trainingplatform_backend.service;

import java.time.LocalDate;
import java.util.List;

import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutLogRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutLogResponse;

public interface WorkoutLogService {

    List<WorkoutLogResponse> getUserLogs(
        Long userId, Long categoryId, LocalDate from, LocalDate to);

    WorkoutLogResponse getById(Long userId, Long logId);

    WorkoutLogResponse create(Long userId, WorkoutLogRequest request);

    WorkoutLogResponse update(Long userId, Long logId, WorkoutLogRequest request);

    void delete(Long userId, Long logId);

}
