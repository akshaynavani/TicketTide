package com.tickettide.event.domain;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> published() {
        return (root, q, cb) -> cb.equal(root.get("status"), EventStatus.PUBLISHED);
    }

    public static Specification<Event> upcoming(Instant now) {
        return (root, q, cb) -> cb.greaterThan(root.get("startsAt"), now);
    }

    public static Specification<Event> inCity(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        String value = city.trim().toLowerCase(Locale.ROOT);
        return (root, q, cb) -> cb.equal(cb.lower(root.get("city")), value);
    }

    public static Specification<Event> inCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String value = category.trim().toLowerCase(Locale.ROOT);
        return (root, q, cb) -> cb.equal(cb.lower(root.get("category")), value);
    }

    public static Specification<Event> matching(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("venue")), pattern));
    }

    public static Specification<Event> startsBefore(Instant before) {
        if (before == null) {
            return null;
        }
        return (root, q, cb) -> cb.lessThan(root.get("startsAt"), before);
    }
}
