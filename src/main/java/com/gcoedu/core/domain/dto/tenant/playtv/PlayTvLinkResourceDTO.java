package com.gcoedu.core.domain.dto.tenant.playtv;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlayTvLinkResourceDTO {
    private String id;

    @NotBlank
    private String type;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String url;

    private Integer sortOrder;
}
