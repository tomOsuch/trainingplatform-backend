package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Invitation;

@Mapper(componentModel = "spring")
public interface InvitationMapper {

    @Mapping(target = "invitedByEmail", source = "invitedBy.email")
    InvitationResponse toResponse(Invitation invitation);
}
