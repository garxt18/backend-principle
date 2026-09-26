package com.backendprinciple.playground.user;

import com.backendprinciple.playground.auth.RefreshTokenService;
import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokens;

    public UserController(UserService userService, RefreshTokenService refreshTokens) {
        this.userService = userService;
        this.refreshTokens = refreshTokens;
    }

    public record UpdateProfileRequest(@Size(min = 2, max = 80) String displayName,
                                       PreferredLanguage preferredLanguage) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 128) String newPassword) {
    }

    @GetMapping
    public UserDto me(@CurrentUser AuthUser me) {
        return UserDto.from(userService.get(me.id()));
    }

    @PatchMapping
    public UserDto update(@CurrentUser AuthUser me, @Valid @RequestBody UpdateProfileRequest req) {
        return UserDto.from(userService.updateProfile(me.id(), req));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUser AuthUser me, @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(me.id(), req.currentPassword(), req.newPassword());
        // A password change should log out every other device.
        refreshTokens.revokeAllForUser(me.id());
    }
}
