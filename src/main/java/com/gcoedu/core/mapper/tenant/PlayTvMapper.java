package com.gcoedu.core.mapper.tenant;

import com.gcoedu.core.domain.dto.tenant.PlayTvVideoDTO;
import com.gcoedu.core.domain.entity.publics.User;
import com.gcoedu.core.domain.entity.tenant.PlayTvVideo;
import com.gcoedu.core.domain.entity.tenant.PlayTvVideoResource;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class PlayTvMapper {

    public PlayTvVideoDTO toDto(PlayTvVideo entity) {
        return toDto(entity, null);
    }

    public PlayTvVideoDTO toDto(PlayTvVideo entity, User creator) {
        if (entity == null) return null;
        PlayTvVideoDTO dto = new PlayTvVideoDTO();
        dto.setId(entity.getId());
        dto.setUrl(entity.getUrl());
        dto.setTitle(entity.getTitle());
        dto.setEntireMunicipality(entity.getEntireMunicipality());
        
        if (entity.getGrade() != null) {
            PlayTvVideoDTO.SimpleRef gradeRef = new PlayTvVideoDTO.SimpleRef();
            gradeRef.setId(entity.getGrade().getId().toString());
            gradeRef.setName(entity.getGrade().getName());
            dto.setGrade(gradeRef);
        }

        if (entity.getSubject() != null) {
            PlayTvVideoDTO.SimpleRef subjectRef = new PlayTvVideoDTO.SimpleRef();
            subjectRef.setId(entity.getSubject().getId());
            subjectRef.setName(entity.getSubject().getName());
            dto.setSubject(subjectRef);
        }

        dto.setSchools(entity.getVideoSchools().stream().map(vs -> {
            PlayTvVideoDTO.SimpleRef ref = new PlayTvVideoDTO.SimpleRef();
            ref.setId(vs.getSchool().getId());
            ref.setName(vs.getSchool().getName());
            return ref;
        }).collect(Collectors.toList()));

        dto.setClasses(entity.getVideoClasses().stream().map(vc -> {
            PlayTvVideoDTO.SimpleRef ref = new PlayTvVideoDTO.SimpleRef();
            ref.setId(vc.getSchoolClass().getId());
            ref.setName(vc.getSchoolClass().getName());
            return ref;
        }).collect(Collectors.toList()));

        dto.setResources(entity.getResources().stream().map(this::toResourceDto).collect(Collectors.toList()));

        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        if (creator != null) {
            PlayTvVideoDTO.SimpleRef creatorRef = new PlayTvVideoDTO.SimpleRef();
            creatorRef.setId(creator.getId());
            creatorRef.setName(creator.getName());
            dto.setCreatedBy(creatorRef);
        }

        return dto;
    }

    private PlayTvVideoDTO.PlayTvResourceDTO toResourceDto(PlayTvVideoResource r) {
        if (r == null) return null;
        PlayTvVideoDTO.PlayTvResourceDTO dto = new PlayTvVideoDTO.PlayTvResourceDTO();
        dto.setId(r.getId());
        dto.setType(r.getResourceType());
        dto.setTitle(r.getTitle());
        dto.setUrl(r.getUrl());
        dto.setFileName(r.getOriginalFilename());
        dto.setMimeType(r.getContentType());
        dto.setSizeBytes(r.getSizeBytes());
        dto.setSortOrder(r.getSortOrder());
        return dto;
    }
}
