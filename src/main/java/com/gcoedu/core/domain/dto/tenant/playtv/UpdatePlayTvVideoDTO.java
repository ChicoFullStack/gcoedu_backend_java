package com.gcoedu.core.domain.dto.tenant.playtv;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePlayTvVideoDTO {

    @Size(max = 100)
    private String title;

    @JsonIgnore
    private boolean titlePresent;

    @Size(max = 2000)
    private String url;

    private String grade;
    private String subject;
    private Boolean entireMunicipality;

    @Size(max = 200)
    private List<String> schools;

    @Size(max = 500)
    private List<String> classes;

    @Valid
    @Size(max = 50)
    private List<PlayTvLinkResourceDTO> resources;

    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }
}
