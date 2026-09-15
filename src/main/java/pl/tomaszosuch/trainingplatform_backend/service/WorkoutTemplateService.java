package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutTemplateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutTemplateResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutTemplate;

import java.util.List;

public interface WorkoutTemplateService {

    List<WorkoutTemplateResponse> getTemplates(Long userId);

    WorkoutTemplateResponse getTemplate(Long userId, Long templateId);

    WorkoutTemplateResponse createTemplate(Long userId, WorkoutTemplateRequest request);

    WorkoutTemplateResponse updateTemplate(Long userId, Long templateId, WorkoutTemplateRequest request);

    void deleteTemplate(Long userId, Long templateId);
}
