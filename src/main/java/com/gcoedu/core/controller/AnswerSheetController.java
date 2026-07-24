package com.gcoedu.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcoedu.core.domain.dto.tenant.report.NovaRespostaAPI;
import com.gcoedu.core.domain.entity.tenant.AnswerSheetGabarito;
import com.gcoedu.core.repository.tenant.AnswerSheetGabaritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping(value = {"/answer-sheets", "/api/answer-sheets"})
@RequiredArgsConstructor
public class AnswerSheetController {

    private final AnswerSheetGabaritoRepository gabaritoRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/create-gabaritos")
    public ResponseEntity<Map<String, Object>> createGabarito(@RequestBody Map<String, Object> payload) {
        try {
            AnswerSheetGabarito gabarito = new AnswerSheetGabarito();

            // title
            Object titleObj = payload.get("title");
            gabarito.setTitle(titleObj != null ? titleObj.toString() : "Gabarito");

            // num_questions
            Object numQObj = payload.get("num_questions");
            int numQuestions = numQObj != null ? Integer.parseInt(numQObj.toString()) : 0;
            gabarito.setNumQuestions(numQuestions);

            // use_blocks / separate_by_subject
            Object useBlocksObj = payload.get("use_blocks");
            gabarito.setUseBlocks(Boolean.TRUE.equals(useBlocksObj));
            Object sepObj = payload.get("separate_by_subject");
            gabarito.setSeparateBySubject(Boolean.TRUE.equals(sepObj));

            // correct_answers (JSON como string)
            Object correctAnswersObj = payload.get("correct_answers");
            String correctAnswersJson = correctAnswersObj != null
                    ? objectMapper.writeValueAsString(correctAnswersObj)
                    : "{}";
            gabarito.setCorrectAnswers(correctAnswersJson);

            // blocks_config (JSON como string)
            Object blocksConfigObj = payload.get("blocks_config");
            if (blocksConfigObj != null) {
                gabarito.setBlocksConfig(objectMapper.writeValueAsString(blocksConfigObj));
            }

            AnswerSheetGabarito saved = gabaritoRepository.save(gabarito);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "gabarito_id", saved.getId(),
                    "title", saved.getTitle() != null ? saved.getTitle() : "",
                    "num_questions", saved.getNumQuestions()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Erro ao criar gabarito: " + e.getMessage())
            );
        }
    }

    @GetMapping("/resultados-agregados")
    public ResponseEntity<NovaRespostaAPI> getResultadosAgregados(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String escola,
            @RequestParam(required = false) String avaliacao) {

        NovaRespostaAPI response = NovaRespostaAPI.builder()
                .nivel_granularidade(avaliacao != null ? "avaliacao" : (escola != null ? "escola" : "municipio"))
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/correct-new")
    public ResponseEntity<Map<String, Object>> correctNew(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("job_id", "job-correct-" + System.currentTimeMillis()));
    }

    @PostMapping("/process-correction")
    public ResponseEntity<Map<String, Object>> processCorrection(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("success", true, "message", "Correção salva"));
    }

    @GetMapping("/correction-progress/{jobId}")
    public ResponseEntity<Map<String, Object>> getCorrectionProgress(@PathVariable String jobId) {
        return ResponseEntity.ok(Map.of("status", "completed", "progress", 100));
    }

    @GetMapping("/manual-entry")
    public ResponseEntity<Map<String, Object>> getManualEntry(
            @RequestParam(required = false) String gabarito_id,
            @RequestParam(required = false) String test_id,
            @RequestParam String student_id) {
        return ResponseEntity.ok(Map.of(
            "student_id", student_id,
            "answers", Collections.emptyMap()
        ));
    }
}

