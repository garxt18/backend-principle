package com.backendprinciple.playground.planly;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Turns an ordered list of topics into a day-by-day schedule.
 *
 * <p>Greedy "fill the day" algorithm: walk the study days (e.g. Mon-Sat) from the start date and pour
 * topic minutes into each day until it is full. A topic bigger than what is left of a day is split over
 * several days, but never into a leftover sliver shorter than {@link #MIN_CHUNK} minutes.
 * O(topics + days) time.
 *
 * <p>Deliberately free of Spring and JPA so it can be unit tested with plain JUnit.
 */
public final class PlanGenerator {

    /** Do not start a topic with less than half an hour left in the day - start it tomorrow instead. */
    static final int MIN_CHUNK = 30;
    /** A plan asking for more than this per study day is not a plan, it is a wish. */
    public static final int MAX_MINUTES_PER_DAY = 16 * 60;
    private static final int STEP = 15;

    public record TopicSlot(long topicId, int minutes) {
    }

    public record Allocation(long topicId, LocalDate date, int orderInDay, int minutes) {
    }

    /** @param endDate last day with planned work (null for an empty plan) */
    public record Schedule(List<Allocation> allocations, LocalDate endDate, int minutesPerDay) {
    }

    private PlanGenerator() {
    }

    public static Schedule generate(List<TopicSlot> topics, int minutesPerDay, Set<DayOfWeek> studyDays, LocalDate start) {
        if (minutesPerDay <= 0) {
            throw new IllegalArgumentException("minutesPerDay must be positive");
        }
        if (studyDays.isEmpty()) {
            throw new IllegalArgumentException("choose at least one study day");
        }
        List<Allocation> result = new ArrayList<>();
        LocalDate day = nextStudyDay(start, studyDays);
        int left = minutesPerDay;
        int order = 0;
        for (TopicSlot topic : topics) {
            int remaining = topic.minutes();
            while (remaining > 0) {
                boolean sliver = left < Math.min(MIN_CHUNK, minutesPerDay) && remaining > left;
                if (left == 0 || sliver) {
                    day = nextStudyDay(day.plusDays(1), studyDays);
                    left = minutesPerDay;
                    order = 0;
                }
                int take = Math.min(remaining, left);
                result.add(new Allocation(topic.topicId(), day, order++, take));
                remaining -= take;
                left -= take;
            }
        }
        return new Schedule(result, result.isEmpty() ? null : result.getLast().date(), minutesPerDay);
    }

    /**
     * Deadline mode: the smallest daily time (in 15-minute steps) that finishes every topic by
     * {@code deadline}. Returns a schedule whose minutesPerDay may exceed {@link #MAX_MINUTES_PER_DAY};
     * the caller decides whether that is acceptable.
     */
    public static Schedule fitDeadline(List<TopicSlot> topics, Set<DayOfWeek> studyDays, LocalDate start, LocalDate deadline) {
        int total = topics.stream().mapToInt(TopicSlot::minutes).sum();
        int days = Math.max(1, countStudyDays(start, deadline, studyDays));
        int perDay = roundUp(Math.max(STEP, (int) Math.ceil(total / (double) days)));
        Schedule s = generate(topics, perDay, studyDays, start);
        // Splitting rules can push the last topic a day later; add 15 minutes until it fits.
        while (s.endDate() != null && s.endDate().isAfter(deadline) && perDay < 24 * 60) {
            perDay += STEP;
            s = generate(topics, perDay, studyDays, start);
        }
        return s;
    }

    /** Study days in [from, to], both inclusive. */
    public static int countStudyDays(LocalDate from, LocalDate to, Set<DayOfWeek> studyDays) {
        int n = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (studyDays.contains(d.getDayOfWeek())) {
                n++;
            }
        }
        return n;
    }

    /** The date on which {@code minutes} of work, starting at {@code from}, would be finished. */
    public static LocalDate finishDate(LocalDate from, int minutes, int minutesPerDay, Set<DayOfWeek> studyDays) {
        LocalDate day = nextStudyDay(from, studyDays);
        int left = minutes;
        while (left > minutesPerDay) {
            left -= minutesPerDay;
            day = nextStudyDay(day.plusDays(1), studyDays);
        }
        return day;
    }

    static LocalDate nextStudyDay(LocalDate from, Set<DayOfWeek> studyDays) {
        LocalDate d = from;
        while (!studyDays.contains(d.getDayOfWeek())) {
            d = d.plusDays(1);
        }
        return d;
    }

    static int roundUp(int minutes) {
        return (minutes + STEP - 1) / STEP * STEP;
    }
}
