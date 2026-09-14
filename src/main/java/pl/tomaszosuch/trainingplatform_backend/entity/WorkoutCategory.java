package pl.tomaszosuch.trainingplatform_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.tomaszosuch.trainingplatform_backend.enums.CategoryIcon;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "workout_category")
public class WorkoutCategory {

    public static final String DEFAULT_COLOR = "#6B7280";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 7, nullable = false)
    @Builder.Default
    private String color = DEFAULT_COLOR;

    @Column(name = "icon_name", nullable = false, length = 255)
    @Builder.Default
    private String iconName = CategoryIcon.DEFAULT.value();

}
