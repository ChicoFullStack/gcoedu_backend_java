package com.gcoedu.core.domain.dto.tenant.playtv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreatePlayTvVideoDTO {

    @NotBlank
    @Size(max = 2000)
    private String url;

    @Size(max = 100)
    private String title;

    @NotBlank
    private String grade;

    @NotBlank
    private String subject;

    private Boolean entireMunicipality = false;

    @Size(max = 200)
    private List<String> schools = new ArrayList<>();

    @Size(max = 500)
    private List<String> classes = new ArrayList<>();

    @Valid
    @Size(max = 50)
    private List<PlayTvLinkResourceDTO> resources = new ArrayList<>();
}
