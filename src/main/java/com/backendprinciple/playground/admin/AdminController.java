package com.backendprinciple.playground.admin;

import com.backendprinciple.playground.auth.RefreshTokenService;
import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import com.backendprinciple.playground.common.web.PageResponse;
import com.backendprinciple.playground.lab.LabProjectRepository;
import com.backendprinciple.playground.planner.StudySessionRepository;
import com.backendprinciple.playground.user.Role;
import com.backendprinciple.playground.user.User;
import com.backendprinciple.playground.user.UserDto;
import com.backendprinciple.playground.user.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only endpoints. Protected by the URL rule in SecurityConfig ("/api/admin/**" needs ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;
    private final LabProjectRepository projects;
    private final StudySessionRepository sessions;
    private final RefreshTokenService refreshTokens;
    private final Clock clock;

    public AdminController(UserRepository users, LabProjectRepository projects, StudySessionRepository sessions,
                           RefreshTokenService refreshTokens, Clock clock) {
        this.users = users;
        this.projects = projects;
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    public record Stats(long users, long labProjects, long activeLearnersLast7Days) {
    }

    public record UpdateUserRequest(Boolean enabled, Role role) {
    }

    @GetMapping("/stats")
    public Stats stats() {
        LocalDate weekAgo = LocalDate.now(clock).minusDays(7);
        return new Stats(users.count(), projects.count(), sessions.countDistinctUsersSince(weekAgo));
    }

    @GetMapping("/users")
    public PageResponse<UserDto> users(@RequestParam(defaultValue = "") String q,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("createdAt").descending());
        return PageResponse.of(
                users.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(q, q, pageable), UserDto::from);
    }

    @PatchMapping("/users/{id}")
    @Transactional
    public UserDto updateUser(@CurrentUser AuthUser me, @PathVariable UUID id, @RequestBody UpdateUserRequest req) {
        if (me.id().equals(id)) {
            throw ApiException.badRequest("Admins cannot change their own role or status");
        }
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        if (req.enabled() != null) {
            user.setEnabled(req.enabled());
            if (!req.enabled()) {
                refreshTokens.revokeAllForUser(id);
            }
        }
        if (req.role() != null) {
            user.setRole(req.role());
        }
        return UserDto.from(user);
    }
}
