package com.gcoedu.core.service.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class EvaluationCalculator {

    public enum CourseLevel {
        EDUCACAO_INFANTIL("educacao_infantil"),
        ANOS_INICIAIS("anos_iniciais"),
        EDUCACAO_ESPECIAL("educacao_especial"),
        EJA("eja"),
        ANOS_FINAIS("anos_finais"),
        ENSINO_MEDIO("ensino_medio");

        private final String value;

        CourseLevel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum SubjectType {
        MATEMATICA("matematica"),
        OUTRAS("outras");

        private final String value;

        SubjectType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Classification {
        ABAIXO_BASICO("Abaixo do Básico"),
        BASICO("Básico"),
        ADEQUADO("Adequado"),
        AVANCADO("Avançado");

        private final String value;

        Classification(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class GradeConfig {
        public final double base;
        public final double divisor;

        public GradeConfig(double base, double divisor) {
            this.base = base;
            this.divisor = divisor;
        }
    }

    public static class ClassificationRange {
        public final double min;
        public final double max;
        public final Classification classification;

        public ClassificationRange(double min, double max, Classification classification) {
            this.min = min;
            this.max = max;
            this.classification = classification;
        }
    }

    // Helper for round to two decimals
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public CourseLevel determineCourseLevel(String courseName) {
        if (courseName == null) {
            return CourseLevel.ANOS_INICIAIS;
        }
        String courseLower = courseName.toLowerCase();
        if (courseLower.contains("infantil")) {
            return CourseLevel.EDUCACAO_INFANTIL;
        } else if (courseLower.contains("iniciais") || (courseLower.contains("fundamental") && courseLower.contains("i ") || courseLower.endsWith("i"))) {
            // Note: handles fundamental i
            return CourseLevel.ANOS_INICIAIS;
        } else if (courseLower.contains("especial")) {
            return CourseLevel.EDUCACAO_ESPECIAL;
        } else if (courseLower.contains("eja")) {
            return CourseLevel.EJA;
        } else if (courseLower.contains("finais") || (courseLower.contains("fundamental") && courseLower.contains("ii"))) {
            return CourseLevel.ANOS_FINAIS;
        } else if (courseLower.contains("médio") || courseLower.contains("medio")) {
            return CourseLevel.ENSINO_MEDIO;
        } else {
            log.warn("Curso não identificado: {}. Usando Anos Iniciais como padrão.", courseName);
            return CourseLevel.ANOS_INICIAIS;
        }
    }

    public SubjectType determineSubjectType(String subjectName) {
        if (subjectName == null) {
            return SubjectType.OUTRAS;
        }
        String subjectLower = subjectName.toLowerCase();
        if (subjectLower.contains("matemática") || subjectLower.contains("matematica")) {
            return SubjectType.MATEMATICA;
        } else {
            return SubjectType.OUTRAS;
        }
    }

    public SubjectType resolveSubjectTypeForGeneral(Boolean hasMatematica) {
        if (hasMatematica == null) {
            return null;
        }
        return hasMatematica ? SubjectType.MATEMATICA : SubjectType.OUTRAS;
    }

    public double getMaxProficiency(CourseLevel courseLevel, SubjectType subjectType) {
        boolean isEarlyStage = courseLevel == CourseLevel.EDUCACAO_INFANTIL ||
                courseLevel == CourseLevel.ANOS_INICIAIS ||
                courseLevel == CourseLevel.EDUCACAO_ESPECIAL ||
                courseLevel == CourseLevel.EJA;

        if (isEarlyStage) {
            return subjectType == SubjectType.MATEMATICA ? 375.0 : 350.0;
        } else {
            return subjectType == SubjectType.MATEMATICA ? 425.0 : 400.0;
        }
    }

    public GradeConfig getGradeConfig(CourseLevel courseLevel, SubjectType subjectType) {
        boolean isEarlyStage = courseLevel == CourseLevel.EDUCACAO_INFANTIL ||
                courseLevel == CourseLevel.ANOS_INICIAIS ||
                courseLevel == CourseLevel.EDUCACAO_ESPECIAL ||
                courseLevel == CourseLevel.EJA;

        if (isEarlyStage) {
            if (subjectType == SubjectType.MATEMATICA) {
                return new GradeConfig(60.0, 262.0);
            } else {
                return new GradeConfig(49.0, 275.0);
            }
        } else {
            // Anos Finais e Ensino Médio - Todas as matérias
            return new GradeConfig(100.0, 300.0);
        }
    }

    public double calculateProficiency(int correctAnswers, int totalQuestions, String courseName, String subjectName) {
        if (totalQuestions == 0) {
            return 0.0;
        }

        CourseLevel courseLevel = determineCourseLevel(courseName);
        SubjectType subjectType = determineSubjectType(subjectName);
        double maxProficiency = getMaxProficiency(courseLevel, subjectType);

        double accuracyRate = (double) correctAnswers / totalQuestions;
        double proficiency = accuracyRate * maxProficiency;

        // Garantir que a proficiência não exceda o máximo permitido
        proficiency = Math.min(proficiency, maxProficiency);

        return roundToTwoDecimals(proficiency);
    }

    public double calculateGrade(double proficiency, String courseName, String subjectName,
                                 boolean useSimpleCalculation, Integer correctAnswers, Integer totalQuestions,
                                 Boolean hasMatematica) {
        // Cálculo simples: acertos/total × 10
        if (useSimpleCalculation && correctAnswers != null && totalQuestions != null) {
            if (totalQuestions == 0) {
                return 0.0;
            }
            double simpleGrade = ((double) correctAnswers / totalQuestions) * 10.0;
            return roundToTwoDecimals(simpleGrade);
        }

        // Cálculo complexo baseado na proficiência
        CourseLevel courseLevel = determineCourseLevel(courseName);
        SubjectType subjectType;
        if ("GERAL".equalsIgnoreCase(subjectName)) {
            SubjectType overrideSubjectType = resolveSubjectTypeForGeneral(hasMatematica);
            subjectType = overrideSubjectType != null ? overrideSubjectType : determineSubjectType(subjectName);
        } else {
            subjectType = determineSubjectType(subjectName);
        }

        GradeConfig config = getGradeConfig(courseLevel, subjectType);

        // Fórmula: (Proficiência - base) / divisor × 10
        double grade = ((proficiency - config.base) / config.divisor) * 10.0;

        // Limitar entre 0.0 e 10.0
        grade = Math.max(0.0, Math.min(10.0, grade));

        return roundToTwoDecimals(grade);
    }

    public String determineClassification(double proficiency, String courseName, String subjectName, Boolean hasMatematica) {
        CourseLevel courseLevel = determineCourseLevel(courseName);
        SubjectType subjectType;
        if ("GERAL".equalsIgnoreCase(subjectName)) {
            SubjectType overrideSubjectType = resolveSubjectTypeForGeneral(hasMatematica);
            subjectType = overrideSubjectType != null ? overrideSubjectType : determineSubjectType(subjectName);
        } else {
            subjectType = determineSubjectType(subjectName);
        }

        ClassificationRange[] ranges;

        if ("GERAL".equalsIgnoreCase(subjectName)) {
            if (hasMatematica != null) {
                ranges = getClassificationRanges(courseLevel, subjectType);
            } else {
                // Legado: faixas específicas para "GERAL"
                if (courseLevel == CourseLevel.ANOS_FINAIS || courseLevel == CourseLevel.ENSINO_MEDIO) {
                    ranges = new ClassificationRange[] {
                        new ClassificationRange(0.0, 224.99, Classification.ABAIXO_BASICO),
                        new ClassificationRange(225.0, 299.99, Classification.BASICO),
                        new ClassificationRange(300.0, 349.99, Classification.ADEQUADO),
                        new ClassificationRange(350.0, 425.00, Classification.AVANCADO)
                    };
                } else {
                    ranges = new ClassificationRange[] {
                        new ClassificationRange(0.0, 174.0, Classification.ABAIXO_BASICO),
                        new ClassificationRange(175.0, 224.0, Classification.BASICO),
                        new ClassificationRange(225.0, 274.0, Classification.ADEQUADO),
                        new ClassificationRange(275.0, 375.0, Classification.AVANCADO)
                    };
                }
            }
        } else {
            ranges = getClassificationRanges(courseLevel, subjectType);
        }

        for (ClassificationRange range : ranges) {
            if (proficiency >= range.min && proficiency <= range.max) {
                return range.classification.getValue();
            }
        }

        return Classification.ABAIXO_BASICO.getValue();
    }

    private ClassificationRange[] getClassificationRanges(CourseLevel courseLevel, SubjectType subjectType) {
        boolean isEarlyStage = courseLevel == CourseLevel.EDUCACAO_INFANTIL ||
                courseLevel == CourseLevel.ANOS_INICIAIS ||
                courseLevel == CourseLevel.EDUCACAO_ESPECIAL ||
                courseLevel == CourseLevel.EJA;

        if (isEarlyStage) {
            if (subjectType == SubjectType.MATEMATICA) {
                return new ClassificationRange[] {
                    new ClassificationRange(0.0, 174.0, Classification.ABAIXO_BASICO),
                    new ClassificationRange(175.0, 224.0, Classification.BASICO),
                    new ClassificationRange(225.0, 274.0, Classification.ADEQUADO),
                    new ClassificationRange(275.0, 375.0, Classification.AVANCADO)
                };
            } else {
                return new ClassificationRange[] {
                    new ClassificationRange(0.0, 149.0, Classification.ABAIXO_BASICO),
                    new ClassificationRange(150.0, 199.0, Classification.BASICO),
                    new ClassificationRange(200.0, 249.0, Classification.ADEQUADO),
                    new ClassificationRange(250.0, 350.0, Classification.AVANCADO)
                };
            }
        } else {
            if (subjectType == SubjectType.MATEMATICA) {
                return new ClassificationRange[] {
                    new ClassificationRange(0.0, 224.99, Classification.ABAIXO_BASICO),
                    new ClassificationRange(225.0, 299.99, Classification.BASICO),
                    new ClassificationRange(300.0, 349.99, Classification.ADEQUADO),
                    new ClassificationRange(350.0, 425.0, Classification.AVANCADO)
                };
            } else {
                return new ClassificationRange[] {
                    new ClassificationRange(0.0, 199.0, Classification.ABAIXO_BASICO),
                    new ClassificationRange(200.0, 274.99, Classification.BASICO),
                    new ClassificationRange(275.0, 324.99, Classification.ADEQUADO),
                    new ClassificationRange(325.0, 400.0, Classification.AVANCADO)
                };
            }
        }
    }

    public Map<String, Object> calculateCompleteEvaluation(int correctAnswers, int totalQuestions,
                                                           String courseName, String subjectName,
                                                           boolean useSimpleCalculation) {
        double proficiency = calculateProficiency(correctAnswers, totalQuestions, courseName, subjectName);
        double grade = calculateGrade(proficiency, courseName, subjectName, useSimpleCalculation,
                correctAnswers, totalQuestions, null);
        String classification = determineClassification(proficiency, courseName, subjectName, null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("proficiency", proficiency);
        result.put("grade", grade);
        result.put("classification", classification);
        result.put("correct_answers", correctAnswers);
        result.put("total_questions", totalQuestions);

        double accuracyRate = totalQuestions > 0 ? ((double) correctAnswers / totalQuestions) * 100.0 : 0.0;
        result.put("accuracy_rate", roundToTwoDecimals(accuracyRate));

        return result;
    }
}
