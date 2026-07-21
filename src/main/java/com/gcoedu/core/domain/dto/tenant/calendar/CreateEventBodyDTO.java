package com.gcoedu.core.domain.dto.tenant.calendar;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CreateEventBodyDTO {
    @NotBlank(message = "O título do evento é obrigatório")
    @Size(max = 160, message = "O título deve ter no máximo 160 caracteres")
    private String title;
    @Size(max = 5000, message = "A descrição deve ter no máximo 5000 caracteres")
    private String description;
    @Size(max = 255, message = "O local deve ter no máximo 255 caracteres")
    private String location;
    @NotBlank(message = "A data de início é obrigatória")
    private String start_at;
    @NotBlank(message = "A data de fim é obrigatória")
    private String end_at;
    private boolean all_day;
    private String timezone;
    private String visibility_scope;
    @Valid
    @NotEmpty(message = "Informe ao menos um destinatário")
    private List<CalendarTargetPayload> targets;
    @Valid
    private List<EventResource> resources;
    private Boolean is_published;
    private String recurrence_rule;
    private Map<String, Object> metadata;

    @Data
    public static class CalendarTargetPayload {
        @NotBlank(message = "O tipo do destinatário é obrigatório")
        private String target_type;
        private String target_id;
        private Map<String, List<String>> filters;
    }

    @Data
    public static class EventResource {
        private String id;
        private String type;
        @Size(max = 160, message = "O título do recurso deve ter no máximo 160 caracteres")
        private String title;
        @Size(max = 2048, message = "A URL do recurso deve ter no máximo 2048 caracteres")
        private String url;
        private Integer sort_order;
    }
}
