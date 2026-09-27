package com.backendprinciple.playground.user;

import com.backendprinciple.playground.common.error.ApiException;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
    }

    @Transactional
    public User updateProfile(UUID id, UserController.UpdateProfileRequest req) {
        User user = get(id);
        if (req.displayName() != null) {
            user.rename(req.displayName());
        }
        if (req.preferredLanguage() != null) {
            user.setPreferredLanguage(req.preferredLanguage());
        }
        return user; // dirty checking: Hibernate flushes the change when the transaction commits
    }

    /** Accounts that sign in with Google (no password yet) can set one without a current password. */
    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        User user = get(id);
        if (user.hasPassword() && (currentPassword == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash()))) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
    }
}
