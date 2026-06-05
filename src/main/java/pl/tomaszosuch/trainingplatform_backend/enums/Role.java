package pl.tomaszosuch.trainingplatform_backend.enums;

import java.util.Optional;

import pl.tomaszosuch.trainingplatform_backend.entity.User;

public enum Role {
    USER,
    ADMIN;

    Optional<User> stream() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stream'");
    }
}
