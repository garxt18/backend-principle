package com.backendprinciple.playground.dsa;

import com.backendprinciple.playground.common.error.ApiException;
import com.backendprinciple.playground.roadmap.ResourceLanguage;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DsaService {

    public static final String CACHE = "dsa-sheet";

    private final DsaTopicRepository topics;
    private final DsaProblemRepository problems;
    private final DsaProgressRepository progress;
    private final Clock clock;

    public DsaService(DsaTopicRepository topics, DsaProblemRepository problems, DsaProgressRepository progress,
                      Clock clock) {
        this.topics = topics;
        this.problems = problems;
        this.progress = progress;
        this.clock = clock;
    }

    // ---- catalog (shared, cached) --------------------------------------------------------------------

    public record ResourceDto(String title, String url, String channel, ResourceLanguage language) {
    }

    public record ProblemDto(Long id, String slug, String title, Difficulty difficulty, String url) {
    }

    public record TopicDto(Long id, String slug, String title, String summary, List<ResourceDto> resources,
                           List<ProblemDto> problems) {
    }

    @Cacheable(CACHE)
    @Transactional(readOnly = true)
    public List<TopicDto> catalog() {
        return topics.findAllWithProblems().stream().map(t -> new TopicDto(t.getId(), t.getSlug(), t.getTitle(),
                        t.getSummary(),
                        t.getResources().stream().map(r -> new ResourceDto(r.getTitle(), r.getUrl(), r.getChannel(),
                                r.getLanguage())).toList(),
                        t.getProblems().stream().map(p -> new ProblemDto(p.getId(), p.getSlug(), p.getTitle(),
                                p.getDifficulty(), p.getPracticeUrl())).toList()))
                .toList();
    }

    // ---- per-user sheet ----------------------------------------------------------------------------

    public record ProblemState(Long id, String slug, String title, Difficulty difficulty, String url, boolean solved,
                               boolean revision, String notes, Instant solvedAt) {
    }

    public record TopicState(Long id, String slug, String title, String summary, List<ResourceDto> resources,
                             int total, int solved, List<ProblemState> problems) {
    }

    public record Count(int total, int solved) {
    }

    public record Stats(int total, int solved, Count easy, Count medium, Count hard, int revision,
                        int solvedLast7Days) {
    }

    public record Sheet(Stats stats, List<TopicState> topics) {
    }

    public record UpdateRequest(Boolean solved, Boolean revision, String notes) {
    }

    @Transactional(readOnly = true)
    public Sheet sheet(UUID userId) {
        Map<Long, DsaProgress> byProblem = progress.findByUserId(userId).stream()
                .collect(Collectors.toMap(DsaProgress::getProblemId, Function.identity()));
        List<TopicState> topicStates = catalog().stream().map(t -> {
            List<ProblemState> states = t.problems().stream().map(p -> {
                DsaProgress s = byProblem.get(p.id());
                return new ProblemState(p.id(), p.slug(), p.title(), p.difficulty(), p.url(),
                        s != null && s.isSolved(), s != null && s.isRevision(), s == null ? null : s.getNotes(),
                        s == null ? null : s.getSolvedAt());
            }).toList();
            int solved = (int) states.stream().filter(ProblemState::solved).count();
            return new TopicState(t.id(), t.slug(), t.title(), t.summary(), t.resources(), states.size(), solved, states);
        }).toList();

        List<ProblemState> all = topicStates.stream().flatMap(t -> t.problems().stream()).toList();
        Instant weekAgo = clock.instant().minus(7, ChronoUnit.DAYS);
        Stats stats = new Stats(all.size(),
                (int) all.stream().filter(ProblemState::solved).count(),
                count(all, Difficulty.EASY), count(all, Difficulty.MEDIUM), count(all, Difficulty.HARD),
                (int) all.stream().filter(ProblemState::revision).count(),
                (int) all.stream().filter(p -> p.solvedAt() != null && p.solvedAt().isAfter(weekAgo)).count());
        return new Sheet(stats, topicStates);
    }

    private static Count count(List<ProblemState> all, Difficulty d) {
        List<ProblemState> ofDifficulty = all.stream().filter(p -> p.difficulty() == d).toList();
        return new Count(ofDifficulty.size(), (int) ofDifficulty.stream().filter(ProblemState::solved).count());
    }

    @Transactional
    public ProblemState update(UUID userId, Long problemId, UpdateRequest req) {
        DsaProblem problem = problems.findById(problemId).orElseThrow(() -> ApiException.notFound("Problem"));
        Instant now = clock.instant();
        DsaProgress p = progress.findByUserIdAndProblemId(userId, problemId)
                .orElseGet(() -> new DsaProgress(userId, problemId));
        if (req.solved() != null) {
            p.markSolved(req.solved(), now);
        }
        if (req.revision() != null) {
            p.setRevision(req.revision());
        }
        if (req.notes() != null) {
            p.setNotes(req.notes().isBlank() ? null : req.notes());
        }
        p.touch(now);
        progress.save(p);
        return new ProblemState(problem.getId(), problem.getSlug(), problem.getTitle(), problem.getDifficulty(),
                problem.getPracticeUrl(), p.isSolved(), p.isRevision(), p.getNotes(), p.getSolvedAt());
    }

    /** Problems solved per UTC day since {@code from} - used by Planly's weekly DSA targets. */
    @Transactional(readOnly = true)
    public Map<LocalDate, Long> solvedPerDay(UUID userId, LocalDate from) {
        return progress.findSolvedTimesSince(userId, from.atStartOfDay(ZoneOffset.UTC).toInstant()).stream()
                .collect(Collectors.groupingBy(i -> i.atZone(ZoneOffset.UTC).toLocalDate(), Collectors.counting()));
    }
}
