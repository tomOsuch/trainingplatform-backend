package pl.tomaszosuch.trainingplatform_backend.mapper;

import org.mapstruct.Mapper;

import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pl.tomaszosuch.trainingplatform_backend.dto.response.AdminUserResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.NotificationPreferencesResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {
    UserResponse toResponse(User user);

    NotificationPreferencesResponse toNotificationPreferences(User user);

    @Mapping(target = "active", source = "isActive")
    AdminUserResponse toAdminResponse(User user);

}
