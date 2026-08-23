package pl.tomaszosuch.trainingplatform_backend.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutCategoryRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutCategoryResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.WorkoutCategoryMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.TrainingPlanRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutLogRepository;
import pl.tomaszosuch.trainingplatform_backend.service.WorkoutCategoryService;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutCategoryServiceImpl implements WorkoutCategoryService {
    
    private final WorkoutCategoryRepository workoutCategoryRepository;
    private final WorkoutCategoryMapper workoutCategoryMapper;
    private final TrainingPlanRepository trainingPlanRepository;
    private final WorkoutLogRepository workoutLogRepository;
    
    @Override
    @Transactional(readOnly = true)  
    public List<WorkoutCategoryResponse> getAllCategories() {
        
        return workoutCategoryRepository.findAll().stream()
            .map(workoutCategoryMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkoutCategoryResponse getCategoryById(Long id) {
        return workoutCategoryRepository.findById(id)
            .map(workoutCategoryMapper::toResponse)
            .orElseThrow(() -> new WorkoutCategoryNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkoutCategoryResponse getCategoryByName(String name) {
        
        return workoutCategoryRepository.findAll().stream()
            .filter(category -> category.getName().equalsIgnoreCase(name))
            .findFirst()
            .map(workoutCategoryMapper::toResponse)
            .orElseThrow(() -> new WorkoutCategoryNotFoundException(name));
    }

    @Override
    public WorkoutCategoryResponse createCategory(WorkoutCategoryRequest request) {
        
        if (workoutCategoryRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Kategoria treningu o nazwie " + request.name() + " już istnieje");
        }

        var category = new WorkoutCategory().builder()
            .name(request.name())
            .color(request.color())
            .iconName(request.iconName())
            .build();

        return workoutCategoryMapper.toResponse(workoutCategoryRepository.save(category));
    }

    @Override
    public WorkoutCategoryResponse updateCategory(Long id, WorkoutCategoryRequest request) {
        var category = workoutCategoryRepository.findById(id)
            .orElseThrow(() -> new WorkoutCategoryNotFoundException(id));

        if (!category.getName().equalsIgnoreCase(request.name()) && workoutCategoryRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Kategoria treningu o nazwie " + request.name() + " już istnieje");
        }

        category.setName(request.name());
        category.setColor(request.color());
        category.setIconName(request.iconName());

        return workoutCategoryMapper.toResponse(workoutCategoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long id) {
        WorkoutCategory category = workoutCategoryRepository.findById(id).orElseThrow(() -> new WorkoutCategoryNotFoundException(id));

        if (trainingPlanRepository.existsByCategoryId(id) || workoutLogRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException(
                "Nie można usunąć kategorii, która jest używana przez treningi");
        }

        workoutCategoryRepository.delete(category);
    }

}
