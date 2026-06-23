package pl.tomaszosuch.trainingplatform_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutCategoryRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutCategoryResponse;
import pl.tomaszosuch.trainingplatform_backend.service.WorkoutCategoryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;



@RestController
@RequestMapping("/workout-categories")
@RequiredArgsConstructor
public class WorkoutCategoryController {

    private final WorkoutCategoryService workoutCategoryService;

    @GetMapping
    public ResponseEntity<List<WorkoutCategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(workoutCategoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkoutCategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(workoutCategoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkoutCategoryResponse> createCategory(@Valid @RequestBody WorkoutCategoryRequest request) {
        return ResponseEntity.ok(workoutCategoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkoutCategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody WorkoutCategoryRequest request) {
        return ResponseEntity.ok(workoutCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        workoutCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
    
}
