package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;

import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

}
