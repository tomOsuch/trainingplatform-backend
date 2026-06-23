package pl.tomaszosuch.trainingplatform_backend.service;

import java.util.List;

import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutCategoryRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutCategoryResponse;

public interface WorkoutCategoryService {

    List<WorkoutCategoryResponse> getAllCategories();
    WorkoutCategoryResponse getCategoryById(Long id);
    WorkoutCategoryResponse getCategoryByName(String name);
    WorkoutCategoryResponse createCategory(WorkoutCategoryRequest request);
    WorkoutCategoryResponse updateCategory(Long id, WorkoutCategoryRequest request);
    void deleteCategory(Long id);
}
