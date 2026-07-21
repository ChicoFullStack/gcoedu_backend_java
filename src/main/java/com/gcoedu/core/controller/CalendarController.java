package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.calendar.CalendarEventDTO;
import com.gcoedu.core.domain.dto.tenant.calendar.CreateEventBodyDTO;
import com.gcoedu.core.service.tenant.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"/calendar", "/api/calendar"})
@RequiredArgsConstructor
public class CalendarController {

    private static final String MANAGER_ROLES =
            "hasAnyRole('ADMIN', 'TECADM', 'DIRETOR', 'COORDENADOR', 'PROFESSOR')";

    private final CalendarService calendarService;

    @GetMapping("/my-events")
    public ResponseEntity<List<CalendarEventDTO>> getMyEvents(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false, name = "exclude_kind") String excludeKind
    ) {
        return ResponseEntity.ok(calendarService.listMyEvents(start, end, kind, excludeKind));
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<Map<String, CalendarEventDTO>> getEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(Map.of("event", calendarService.getEvent(eventId)));
    }

    @PostMapping("/events")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<Map<String, CalendarEventDTO>> createEvent(
            @Valid @RequestBody CreateEventBodyDTO body
    ) {
        return ResponseEntity.ok(Map.of("event", calendarService.createEvent(body)));
    }

    @PutMapping("/events/{eventId}")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<Map<String, CalendarEventDTO>> updateEvent(
            @PathVariable String eventId,
            @Valid @RequestBody CreateEventBodyDTO body
    ) {
        return ResponseEntity.ok(Map.of("event", calendarService.updateEvent(eventId, body)));
    }

    @DeleteMapping("/events/{eventId}")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<Map<String, Boolean>> deleteEvent(@PathVariable String eventId) {
        calendarService.deleteEvent(eventId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/events/{eventId}/publish")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<Map<String, CalendarEventDTO>> publishEvent(@PathVariable String eventId) {
        return ResponseEntity.ok(Map.of("event", calendarService.publishEvent(eventId)));
    }

    @GetMapping("/targets/me")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getTargets() {
        return ResponseEntity.ok(calendarService.getTargetsForCurrentUser());
    }

    @PostMapping(
            value = "/events/{eventId}/resources/file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<Map<String, Object>> uploadFileResource(
            @PathVariable String eventId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false, name = "sort_order") Integer sortOrder
    ) {
        return ResponseEntity.ok(Map.of(
                "resource",
                calendarService.uploadFileResource(eventId, file, title, sortOrder)
        ));
    }

    @GetMapping("/events/{eventId}/resources/{resourceId}/download")
    public ResponseEntity<ByteArrayResource> downloadFileResource(
            @PathVariable String eventId,
            @PathVariable String resourceId
    ) {
        CalendarService.FileDownload download =
                calendarService.downloadFileResource(eventId, resourceId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.contentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(download.contentType());
            } catch (IllegalArgumentException ignored) {
                // Conteúdo legado sem MIME válido: usar application/octet-stream.
            }
        }
        String fileName = download.fileName() == null ? "anexo" : download.fileName();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.content().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(new ByteArrayResource(download.content()));
    }

    @DeleteMapping("/events/{eventId}/resources/{resourceId}")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<Map<String, Boolean>> deleteResource(
            @PathVariable String eventId,
            @PathVariable String resourceId
    ) {
        calendarService.deleteResource(eventId, resourceId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/events/{eventId}/read")
    public ResponseEntity<Map<String, Boolean>> markRead(@PathVariable String eventId) {
        calendarService.assertCanRead(eventId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/events/{eventId}/recipients")
    @PreAuthorize(MANAGER_ROLES)
    public ResponseEntity<Map<String, Object>> listRecipients(
            @PathVariable String eventId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50", name = "per_page") int perPage
    ) {
        calendarService.getEvent(eventId);
        return ResponseEntity.ok(Map.of(
                "items", List.of(),
                "page", Math.max(1, page),
                "per_page", Math.max(1, Math.min(100, perPage)),
                "total", 0
        ));
    }
}
