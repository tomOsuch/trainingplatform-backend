package pl.tomaszosuch.trainingplatform_backend.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;

public class WorkoutLogSpecifications {

    private WorkoutLogSpecifications() {

    }

    public static Specification<WorkoutLog> matchingGoal(Goal goal) {
        return (root, query, cb) -> {
            root.fetch("category", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), goal.getUser().getId()));
            predicates.add(cb.greaterThanOrEqualTo(root.get("performedDate"), goal.getStartDate()));

            if (goal.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("performedDate"), goal.getEndDate()));
            }
            if (goal.getCategory() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), goal.getCategory().getId()));
            }
            if (goal.isAchieved()) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), goal.getAchievedAt()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
