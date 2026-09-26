package com.backendprinciple.playground.auth;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.user.PreferredLanguage;
import com.backendprinciple.playground.user.Role;
import com.backendprinciple.playground.user.User;
import com.backendprinciple.playground.user.UserDto;
import com.backendprinciple.playground.user.UserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokens;
    /** Hash of a random string: used to spend the same CPU time when the email does not exist. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       TokenService tokenService, RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokens = refreshTokens;
        this.dummyHash = passwordEncoder.encode("timing-attack-padding");
    }

    /** Everything the controller needs to build the response and the refresh cookie. */
    public record Session(String accessToken, long expiresIn, String refreshToken, UserDto user) {
    }

    @Transactional
    public Session register(String email, String password, String displayName, PreferredLanguage language) {
        String normalized = User.normalizeEmail(email);
        if (users.existsByEmail(normalized)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = new User(normalized, passwordEncoder.encode(password), displayName, Role.USER);
        if (language != null) {
            user.setPreferredLanguage(language);
        }
        users.save(user);
        return startSession(user);
    }

    @Transactional
    public Session login(String email, String password) {
        Optional<User> found = users.findByEmail(User.normalizeEmail(email));
        // Always run one BCrypt comparison so response time does not reveal whether the email exists.
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean ok = passwordEncoder.matches(password, hash);
        if (found.isEmpty() || !ok) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        User user = found.get();
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("This account is disabled");
        }
        return startSession(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Session refresh(String rawRefreshToken) {
        RefreshTokenService.Issued rotated = refreshTokens.rotate(rawRefreshToken);
        User user = users.findById(rotated.userId())
                .filter(User::isEnabled)
                .orElseThrow(() -> ApiException.unauthorized("Session expired - please log in again"));
        return new Session(tokenService.issueAccessToken(user), tokenService.accessTokenTtlSeconds(),
                rotated.rawToken(), UserDto.from(user));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    private Session startSession(User user) {
        RefreshTokenService.Issued refresh = refreshTokens.issueNewFamily(user.getId());
        return new Session(tokenService.issueAccessToken(user), tokenService.accessTokenTtlSeconds(),
                refresh.rawToken(), UserDto.from(user));
    }
}
