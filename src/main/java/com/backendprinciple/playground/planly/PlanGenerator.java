package com.backendprinciple.playground.planly;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns an ordered list of topics into a week-by-week schedule.
 *
 * <p>Greedy "fill the week" algorithm: walk topics in roadmap order and pour their hours into the
 * current week until it is full, then continue in the next week. A topic bigger than the remaining
 * space is split across weeks. O(topics + weeks) time.
 *
 * <p>Deliberately free of Spring and JPA so it can be unit tested with plain JUnit.
 */
public final class PlanGenerator {

    public record TopicSlot(long topicId, int hours) {
    }

    public record Allocation(long topicId, int week, int orderInWeek, int hours) {
    }

    public record Schedule(List<Allocation> allocations, int totalWeeks) {
    }

    private PlanGenerator() {
    }

    public static Schedule generate(List<TopicSlot> topics, int hoursPerWeek) {
        if (hoursPerWeek <= 0) {
            throw new IllegalArgumentException("hoursPerWeek must be positive");
        }
        List<Allocation> result = new ArrayList<>();
        int week = 1;
        int capacity = hoursPerWeek;
        int order = 0;
        for (TopicSlot topic : topics) {
            int remaining = topic.hours();
            while (remaining > 0) {
                int take = Math.min(remaining, capacity);
                result.add(new Allocation(topic.topicId(), week, order++, take));
                remaining -= take;
                capacity -= take;
                if (capacity == 0) {
                    week++;
                    capacity = hoursPerWeek;
                    order = 0;
                }
            }
        }
        // If the last week was filled exactly, the loop already advanced to an empty week.
        int totalWeeks = result.isEmpty() ? 0 : result.getLast().week();
        return new Schedule(result, totalWeeks);
    }
}
