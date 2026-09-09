package pl.tomaszosuch.trainingplatform_backend.repository;

import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;

public interface PlanStatusCountView {

    PlanStatus getStatus();

    Long getCount();
}
