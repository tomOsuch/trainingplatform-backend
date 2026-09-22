package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pl.tomaszosuch.trainingplatform_backend.dto.response.AthleteResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CooperationMapper {

    @Mapping(target = "id", source = "athlete.id")
    @Mapping(target = "firstName", source = "athlete.firstName")
    @Mapping(target = "lastName", source = "athlete.lastName")
    @Mapping(target = "email", source = "athlete.email")
    @Mapping(target = "cooperationSince", source = "respondedAt")
    AthleteResponse toAthleteResponse(Cooperation cooperation);

}
