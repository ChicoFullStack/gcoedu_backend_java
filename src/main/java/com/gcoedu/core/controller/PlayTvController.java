package com.gcoedu.core.controller;

import com.gcoedu.core.domain.dto.tenant.PlayTvVideoDTO;
import com.gcoedu.core.domain.dto.tenant.playtv.CreatePlayTvVideoDTO;
import com.gcoedu.core.domain.dto.tenant.playtv.UpdatePlayTvVideoDTO;
import com.gcoedu.core.service.tenant.PlayTvService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(value = {"/play-tv", "/api/play-tv"})
@RequiredArgsConstructor
public class PlayTvController {

    private final PlayTvService playTvService;

    @GetMapping({"/videos", "/videos/"})
    public ResponseEntity<List<PlayTvVideoDTO>> getVideos(
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String subject
    ) {
        return ResponseEntity.ok(playTvService.findVisible(school, grade, subject));
    }

    @GetMapping("/videos/{videoId}")
    public ResponseEntity<PlayTvVideoDTO> getVideo(@PathVariable String videoId) {
        return ResponseEntity.ok(playTvService.findVisibleById(videoId));
    }

    @PostMapping("/videos")
    public ResponseEntity<PlayTvVideoDTO> createVideo(
            @Valid @RequestBody CreatePlayTvVideoDTO body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playTvService.create(body));
    }

    @PutMapping("/videos/{videoId}")
    public ResponseEntity<PlayTvVideoDTO> updateVideo(
            @PathVariable String videoId,
            @Valid @RequestBody UpdatePlayTvVideoDTO body
    ) {
        return ResponseEntity.ok(playTvService.update(videoId, body));
    }

    @DeleteMapping("/videos/{videoId}")
    public ResponseEntity<Void> deleteVideo(@PathVariable String videoId) {
        playTvService.delete(videoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/videos/{videoId}/resources/file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PlayTvVideoDTO.PlayTvResourceDTO> uploadFileResource(
            @PathVariable String videoId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(name = "sort_order", required = false) Integer sortOrder
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                playTvService.uploadFileResource(videoId, file, title, sortOrder)
        );
    }

    @GetMapping("/videos/{videoId}/resources/{resourceId}/download")
    public ResponseEntity<byte[]> downloadFileResource(
            @PathVariable String videoId,
            @PathVariable String resourceId
    ) {
        PlayTvService.FileDownload download =
                playTvService.downloadFileResource(videoId, resourceId);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(download.contentType());
        } catch (IllegalArgumentException exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        String disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentLength(download.content().length)
                .body(download.content());
    }

    @DeleteMapping("/videos/{videoId}/resources/{resourceId}")
    public ResponseEntity<Void> deleteResource(
            @PathVariable String videoId,
            @PathVariable String resourceId
    ) {
        playTvService.deleteResource(videoId, resourceId);
        return ResponseEntity.noContent().build();
    }
}
