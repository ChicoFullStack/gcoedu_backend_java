package com.gcoedu.core.domain.dto.tenant.calendar;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CalendarEventDTO {
    private String id;
    private String title;
    private String start;
    private String end;
    private boolean allDay;
    private String timezone;
    private CalendarCreatedBy created_by;
    private Map<String, Object> extendedProps;

    @Data
    @Builder
    public static class CalendarCreatedBy {
        private String id;
        private String role;
        private String name;
    }
}
