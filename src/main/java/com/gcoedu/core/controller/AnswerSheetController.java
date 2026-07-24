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

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @GetMapping("/gabaritos")
    public ResponseEntity<Map<String, Object>> getGabaritos() {
        java.util.List<AnswerSheetGabarito> all = gabaritoRepository.findAll();
        java.util.List<Map<String, Object>> list = all.stream().map(g -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", g.getId());
            map.put("title", g.getTitle() != null ? g.getTitle() : "");
            map.put("num_questions", g.getNumQuestions() != null ? g.getNumQuestions() : 0);
            return map;
        }).toList();
        return ResponseEntity.ok(Map.of("gabaritos", list));
    }

    @GetMapping("/opcoes-filtros")
    public ResponseEntity<Map<String, Object>> getOpcoesFiltros() {
        return ResponseEntity.ok(Map.of(
            "state", Collections.emptyList(),
            "city", Collections.emptyList(),
            "school", Collections.emptyList(),
            "grade", Collections.emptyList(),
            "class", Collections.emptyList()
        ));
    }

    @GetMapping("/gabarito/{id}")
    public ResponseEntity<Map<String, Object>> getGabaritoDetail(@PathVariable String id) {
        AnswerSheetGabarito g = gabaritoRepository.findById(id).orElse(null);
        if (g == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", g.getId());
        map.put("title", g.getTitle() != null ? g.getTitle() : "");
        map.put("num_questions", g.getNumQuestions() != null ? g.getNumQuestions() : 0);
        map.put("use_blocks", g.getUseBlocks() != null ? g.getUseBlocks() : false);
        map.put("correct_answers", g.getCorrectAnswers() != null ? g.getCorrectAnswers() : "{}");
        map.put("blocks_config", g.getBlocksConfig() != null ? g.getBlocksConfig() : "{}");
        
        return ResponseEntity.ok(map);
    }

    @GetMapping("/gabarito/{id}/students")
    public ResponseEntity<Map<String, Object>> getGabaritoStudents(@PathVariable String id, 
            @RequestParam(required = false) String class_id,
            @RequestParam(required = false) String grade_id,
            @RequestParam(required = false) String school_id,
            @RequestParam(required = false) String flat) {
        
        return ResponseEntity.ok(Map.of(
            "gabarito_id", id,
            "classes", java.util.Collections.emptyList(),
            "students", java.util.Collections.emptyList()
        ));
    }

    @GetMapping("/manual-entry")
    public ResponseEntity<Map<String, Object>> getManualEntry(
            @RequestParam(required = false) String gabarito_id,
            @RequestParam(required = false) String test_id,
            @RequestParam String student_id) {
        return ResponseEntity.ok(Map.of(
            "student_id", student_id,
            "answers", java.util.Collections.emptyMap()
        ));
    }
}

