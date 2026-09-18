package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationInvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CooperationMapper {

    @Mapping(target = "coachId", source = "coach.id")
    @Mapping(target = "coachFirstName", source = "coach.firstName")
    @Mapping(target = "coachLastName", source = "coach.lastName")
    @Mapping(target = "coachEmail", source = "coach.email")
    CooperationInvitationResponse toResponse(Cooperation cooperation);
}
