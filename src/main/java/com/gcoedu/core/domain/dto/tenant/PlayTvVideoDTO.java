package com.gcoedu.core.domain.dto.tenant;

import lombok.Data;
import java.util.List;

@Data
public class PlayTvVideoDTO {
    private String id;
    private String url;
    private String title;
    private Boolean entireMunicipality;
    private SimpleRef grade;
    private SimpleRef subject;
    private List<SimpleRef> schools;
    private List<SimpleRef> classes;
    private List<PlayTvResourceDTO> resources;
    private String createdAt;
    private SimpleRef createdBy;

    @Data
    public static class SimpleRef {
        private String id;
        private String name;
    }

    @Data
    public static class PlayTvResourceDTO {
        private String id;
        private String type; // mapped from resourceType ("link", "file")
        private String title;
        private String url;
        private String fileName; // mapped from originalFilename
        private String mimeType; // mapped from contentType
        private Long sizeBytes;
        private Integer sortOrder;
    }
}
