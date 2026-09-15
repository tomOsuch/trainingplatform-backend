package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutTemplateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutTemplateResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutTemplate;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutTemplateNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.WorkoutTemplateMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutTemplateRepository;
import pl.tomaszosuch.trainingplatform_backend.service.WorkoutTemplateService;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkoutTemplateServiceImpl implements WorkoutTemplateService {

    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final WorkoutTemplateMapper workoutTemplateMapper;
    private final UserRepository userRepository;
    private final WorkoutCategoryRepository workoutCategoryRepository;

    @Override
    public List<WorkoutTemplateResponse> getTemplates(Long userId) {

        return workoutTemplateRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(workoutTemplateMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkoutTemplateResponse getTemplate(Long userId, Long templateId) {
        return workoutTemplateMapper.toResponse(findOwnedTemplate(templateId, userId));
    }

    @Override
    public WorkoutTemplateResponse createTemplate(Long userId, WorkoutTemplateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        WorkoutTemplate template = WorkoutTemplate.builder()
                .user(user)
                .category(requireCategory(request.categoryId()))
                .name(request.name())
                .description(request.description())
                .durationMin(request.durationMin())
                .build();

        return workoutTemplateMapper.toResponse(workoutTemplateRepository.save(template));
    }

    @Override
    public WorkoutTemplateResponse updateTemplate(Long userId, Long templateId, WorkoutTemplateRequest request) {
        WorkoutTemplate template = findOwnedTemplate(templateId, userId);

        template.setCategory(requireCategory(request.categoryId()));
        template.setName(request.name());
        template.setDescription(request.description());
        template.setDurationMin(request.durationMin());

        return workoutTemplateMapper.toResponse(workoutTemplateRepository.save(template));
    }

    @Override
    public void deleteTemplate(Long userId, Long templateId) {
        workoutTemplateRepository.delete(findOwnedTemplate(templateId, userId));
    }

    private WorkoutTemplate findOwnedTemplate(Long templateId, Long userId) {
        WorkoutTemplate template = workoutTemplateRepository.findById(templateId)
                .orElseThrow(() -> new WorkoutTemplateNotFoundException(templateId));

        if (!template.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Brak uprawnień do tego szablonu");
        }
        return template;
    }

    private WorkoutCategory requireCategory(Long categoryId) {
        return workoutCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new WorkoutCategoryNotFoundException(categoryId));
    }
}
