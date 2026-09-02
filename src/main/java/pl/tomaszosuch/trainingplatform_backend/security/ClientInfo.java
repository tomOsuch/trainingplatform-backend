package pl.tomaszosuch.trainingplatform_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

public record ClientInfo(String ip, String userAgent) {

    public static ClientInfo from(HttpServletRequest request) {
        return new ClientInfo(
                request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT));
    }
}
