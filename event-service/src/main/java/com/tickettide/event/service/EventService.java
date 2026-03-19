package com.tickettide.event.service;

import com.tickettide.common.security.AuthenticatedUser;
import com.tickettide.common.security.Role;
import com.tickettide.common.web.ApiException;
import com.tickettide.common.web.PageResponse;
import com.tickettide.event.api.EventDtos.EventRequest;
import com.tickettide.event.api.EventDtos.EventResponse;
import com.tickettide.event.api.EventDtos.EventSummary;
import com.tickettide.event.api.EventDtos.SalesReport;
import com.tickettide.event.api.EventDtos.TicketTypeRequest;
import com.tickettide.event.api.EventDtos.TicketTypeResponse;
import com.tickettide.event.api.EventDtos.TicketTypeSales;
import com.tickettide.event.domain.Event;
import com.tickettide.event.domain.EventRepository;
import com.tickettide.event.domain.EventStatus;
import com.tickettide.event.domain.TicketType;
import com.tickettide.event.storage.ImageStorage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tickettide.event.domain.EventSpecifications.inCategory;
import static com.tickettide.event.domain.EventSpecifications.inCity;
import static com.tickettide.event.domain.EventSpecifications.matching;
import static com.tickettide.event.domain.EventSpecifications.published;
import static com.tickettide.event.domain.EventSpecifications.startsBefore;
import static com.tickettide.event.domain.EventSpecifications.upcoming;

@Service
public class EventService {

    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final EventRepository events;
    private final ImageStorage images;

    public EventService(EventRepository events, ImageStorage images) {
        this.events = events;
        this.images = images;
    }

    @Transactional(readOnly = true)
    public PageResponse<EventSummary> search(String q, String city, String category, Instant before, Pageable page) {
        Specification<Event> spec = Specification.where(published())
                .and(upcoming(Instant.now()))
                .and(matching(q))
                .and(inCity(city))
                .and(inCategory(category))
                .and(startsBefore(before));
        return PageResponse.of(events.findAll(spec, page), this::summary);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventSummary> mine(AuthenticatedUser user, Pageable page) {
        return PageResponse.of(events.findByOrganizerId(user.id(), page), this::summary);
    }

    /** Drafts are only visible to their organizer (and admins). */
    @Transactional(readOnly = true)
    public EventResponse get(UUID id, AuthenticatedUser viewer) {
        Event event = load(id);
        if (event.getStatus() == EventStatus.DRAFT && !canManage(event, viewer)) {
            throw ApiException.notFound("Event");
        }
        return detail(event);
    }

    @Transactional
    public EventResponse create(EventRequest req, AuthenticatedUser user) {
        requireOrganizer(user);
        Event event = guard(() -> new Event(user.id(), req.title(), req.description(), req.venue(), req.city(),
                req.category(), req.startsAt(), req.endsAt()));
        return detail(events.save(event));
    }

    @Transactional
    public EventResponse update(UUID id, EventRequest req, AuthenticatedUser user) {
        Event event = loadOwned(id, user);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw ApiException.conflict("Cancelled events cannot be edited");
        }
        guard(() -> {
            event.update(req.title(), req.description(), req.venue(), req.city(), req.category(),
                    req.startsAt(), req.endsAt());
            return event;
        });
        return detail(event);
    }

    @Transactional
    public TicketTypeResponse addTicketType(UUID eventId, TicketTypeRequest req, AuthenticatedUser user) {
        Event event = loadOwned(eventId, user);
        TicketType type = guard(() -> event.addTicketType(req.name(), req.priceCents(), req.currency(), req.capacity()));
        events.flush();
        return TicketTypeResponse.from(type);
    }

    @Transactional
    public EventResponse publish(UUID id, AuthenticatedUser user) {
        Event event = loadOwned(id, user);
        guard(() -> {
            event.publish();
            return event;
        });
        return detail(event);
    }

    @Transactional
    public EventResponse cancel(UUID id, AuthenticatedUser user) {
        Event event = loadOwned(id, user);
        guard(() -> {
            event.cancel();
            return event;
        });
        return detail(event);
    }

    @Transactional
    public EventResponse uploadImage(UUID id, MultipartFile file, AuthenticatedUser user) {
        Event event = loadOwned(id, user);
        String ext = file.getContentType() == null ? null : IMAGE_TYPES.get(file.getContentType());
        if (file.isEmpty() || ext == null) {
            throw ApiException.badRequest("Upload a non-empty JPEG, PNG or WebP image");
        }
        String key = "events/%s/cover-%s.%s".formatted(event.getId(), UUID.randomUUID(), ext);
        try {
            images.put(key, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String previous = event.getImageKey();
        event.setImageKey(key);
        if (previous != null) {
            images.delete(previous);
        }
        return detail(event);
    }

    @Transactional(readOnly = true)
    public SalesReport sales(UUID id, AuthenticatedUser user) {
        Event event = loadOwned(id, user);
        List<TicketTypeSales> rows = event.getTicketTypes().stream()
                .map(t -> new TicketTypeSales(t.getId(), t.getName(), t.getCapacity(), t.sold(),
                        t.sold() * t.getPriceCents()))
                .toList();
        int totalSold = rows.stream().mapToInt(TicketTypeSales::sold).sum();
        long revenue = rows.stream().mapToLong(TicketTypeSales::revenueCents).sum();
        return new SalesReport(event.getId(), event.getTitle(), rows, totalSold, revenue);
    }

    Event loadOwned(UUID id, AuthenticatedUser user) {
        Event event = load(id);
        if (!canManage(event, user)) {
            throw ApiException.forbidden("You do not manage this event");
        }
        return event;
    }

    private Event load(UUID id) {
        return events.findById(id).orElseThrow(() -> ApiException.notFound("Event"));
    }

    private static boolean canManage(Event event, AuthenticatedUser user) {
        return user != null && (user.isAdmin() || event.getOrganizerId().equals(user.id()));
    }

    private static void requireOrganizer(AuthenticatedUser user) {
        if (!user.hasRole(Role.ORGANIZER)) {
            throw ApiException.forbidden("Only organizers can create events");
        }
    }

    private EventResponse detail(Event e) {
        return EventResponse.from(e, images.publicUrl(e.getImageKey()).orElse(null));
    }

    private EventSummary summary(Event e) {
        return EventSummary.from(e, images.publicUrl(e.getImageKey()).orElse(null));
    }

    /** Translates domain rule violations into HTTP-friendly errors. */
    private static <T> T guard(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            throw ApiException.conflict(e.getMessage());
        }
    }
}
