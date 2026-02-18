package com.sara.tfgdam.service;

import com.sara.tfgdam.dto.CreateModuleRequest;
import com.sara.tfgdam.dto.ExcelImportRequest;
import com.sara.tfgdam.dto.ImportRAItemDto;
import com.sara.tfgdam.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExcelTemplateMapperService {

    private static final String SHEET_DATOS_INICIALES = "Datos Iniciales";
    private static final String SHEET_ACTIVIDADES = "Actividades";

    private static final int RA_ROW_START = 8;   // row 9
    private static final int RA_ROW_END = 17;    // row 18
    private static final int UT_ROW_START = 8;   // row 9
    private static final int UT_ROW_END = 27;    // row 28
    private static final int ACT_ROW_START = 34; // row 35
    private static final int ACT_ROW_END = 63;   // row 64

    private static final int COL_RA_CODE = 0;    // A
    private static final int COL_RA_NAME = 1;    // B
    private static final int COL_RA_WEIGHT = 2;  // C
    private static final int COL_UT_KEY = 4;     // E
    private static final int COL_RA_START = 5;   // F
    private static final int COL_RA_END = 14;    // O
    private static final int COL_ACTIVITY_NAME = 4; // E
    private static final int COL_ACTIVITY_UT = 15;  // P
    private static final int COL_ACTIVITY_EVAL = 16; // Q
    private static final int COL_STUDENT_CODE = 18; // S
    private static final int COL_STUDENT_NAME = 19; // T

    private static final int ACT_BLOCK_START_COL = 2; // C
    private static final int ACT_BLOCK_WIDTH = 11;    // note + 10 exercises
    private static final int ACT_BLOCK_COUNT = 30;
    private static final int ACT_ROW_STUDENT_START = 5; // row 6
    private static final int ACT_ROW_WEIGHTS = 3;       // row 4

    public ExcelImportRequest mapTemplate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("file is required and cannot be empty");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) {
            throw new BusinessValidationException("Only .xlsx files are supported for /imports/excel-file");
        }

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet datos = workbook.getSheet(SHEET_DATOS_INICIALES);
            Sheet actividades = workbook.getSheet(SHEET_ACTIVIDADES);

            if (datos == null || actividades == null) {
                throw new BusinessValidationException("Invalid template. Sheets 'Datos Iniciales' and 'Actividades' are required");
            }

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);

            ExcelImportRequest request = new ExcelImportRequest();
            request.setModule(buildModule(datos, evaluator, formatter));

            List<ImportRAItemDto> ras = parseRAs(datos, evaluator, formatter);
            if (ras.isEmpty()) {
                throw new BusinessValidationException("No RAs found in template");
            }
            request.setRas(ras);

            Map<Integer, String> raCodeByColumn = parseRAColumnHeaders(datos, 7, evaluator, formatter);
            List<ActivityRow> activityRows = parseActivityRows(datos, raCodeByColumn, evaluator, formatter);
            Map<String, Integer> evalByUtKey = buildEvaluationByUT(activityRows);
            Map<String, List<ExcelImportRequest.InstrumentItem>> instrumentsByUtKey = buildInstrumentsByUT(activityRows);

            List<ExcelImportRequest.UTItem> uts = parseUTs(datos, raCodeByColumn, evalByUtKey, instrumentsByUtKey, evaluator, formatter);
            if (uts.isEmpty()) {
                throw new BusinessValidationException("No UTs found in template");
            }
            request.setUts(uts);

            List<ExcelImportRequest.StudentItem> students = parseStudents(datos, actividades, activityRows, evaluator, formatter);
            request.setStudents(students);

            return request;
        } catch (IOException ex) {
            throw new BusinessValidationException("Cannot read excel file: " + ex.getMessage());
        } catch (BusinessValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessValidationException("Cannot parse excel template: " + ex.getMessage());
        }
    }

    private CreateModuleRequest buildModule(Sheet datos, FormulaEvaluator evaluator, DataFormatter formatter) {
        String moduleName = firstNonBlank(
                getString(datos, 2, 1, evaluator, formatter), // B3
                getString(datos, 2, 2, evaluator, formatter), // C3
                getString(datos, 2, 3, evaluator, formatter)  // D3
        );

        if (isBlank(moduleName)) {
            moduleName = "Modulo importado desde Excel";
        }

        int year = Year.now().getValue();
        CreateModuleRequest module = new CreateModuleRequest();
        module.setName(moduleName);
        module.setAcademicYear(year + "-" + (year + 1));
        module.setTeacherName("Docente importado");
        return module;
    }

    private List<ImportRAItemDto> parseRAs(Sheet datos, FormulaEvaluator evaluator, DataFormatter formatter) {
        List<ImportRAItemDto> ras = new ArrayList<>();

        for (int row = RA_ROW_START; row <= RA_ROW_END; row++) {
            String code = trimOrNull(getString(datos, row, COL_RA_CODE, evaluator, formatter));
            String name = trimOrNull(getString(datos, row, COL_RA_NAME, evaluator, formatter));
            BigDecimal weight = getDecimal(datos, row, COL_RA_WEIGHT, evaluator, formatter);

            if (isBlank(code) && isBlank(name) && weight == null) {
                continue;
            }

            if (isBlank(code)) {
                code = "RA" + (ras.size() + 1);
            }
            if (isBlank(name)) {
                name = code;
            }
            if (weight == null) {
                throw new BusinessValidationException("Missing RA weight at row " + (row + 1));
            }

            ImportRAItemDto dto = new ImportRAItemDto();
            dto.setCode(code);
            dto.setName(name);
            dto.setWeightPercent(scale2(weight));
            ras.add(dto);
        }

        return ras;
    }

    private Map<Integer, String> parseRAColumnHeaders(Sheet datos,
                                                      int headerRow,
                                                      FormulaEvaluator evaluator,
                                                      DataFormatter formatter) {
        Map<Integer, String> result = new HashMap<>();
        for (int col = COL_RA_START; col <= COL_RA_END; col++) {
            String code = trimOrNull(getString(datos, headerRow, col, evaluator, formatter));
            if (!isBlank(code)) {
                result.put(col, code);
            }
        }
        return result;
    }

    private List<ActivityRow> parseActivityRows(Sheet datos,
                                                Map<Integer, String> raCodeByColumn,
                                                FormulaEvaluator evaluator,
                                                DataFormatter formatter) {
        List<ActivityRow> rows = new ArrayList<>();
        Map<String, Integer> duplicateCount = new HashMap<>();

        for (int row = ACT_ROW_START; row <= ACT_ROW_END; row++) {
            String activityName = trimOrNull(getString(datos, row, COL_ACTIVITY_NAME, evaluator, formatter));
            String utKey = trimOrNull(getString(datos, row, COL_ACTIVITY_UT, evaluator, formatter));
            BigDecimal evaluation = getDecimal(datos, row, COL_ACTIVITY_EVAL, evaluator, formatter);

            BigDecimal totalWeight = BigDecimal.ZERO;
            List<String> raCodes = new ArrayList<>();

            for (int col = COL_RA_START; col <= COL_RA_END; col++) {
                BigDecimal value = getDecimal(datos, row, col, evaluator, formatter);
                if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                totalWeight = totalWeight.add(value);
                String raCode = raCodeByColumn.get(col);
                if (!isBlank(raCode)) {
                    raCodes.add(raCode);
                }
            }

            boolean hasData = !isBlank(activityName) || !isBlank(utKey) || totalWeight.compareTo(BigDecimal.ZERO) > 0;
            if (!hasData) {
                continue;
            }

            if (isBlank(activityName)) {
                activityName = "Actividad" + (row - ACT_ROW_START + 1);
            }
            if (isBlank(utKey)) {
                throw new BusinessValidationException("Activity '" + activityName + "' has no UT in column P");
            }
            if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessValidationException("Activity '" + activityName + "' has no RA weight > 0");
            }
            if (raCodes.isEmpty()) {
                throw new BusinessValidationException("Activity '" + activityName + "' has no RA associations");
            }

            String keyBase = activityName.trim();
            String normalized = normalize(keyBase);
            int count = duplicateCount.getOrDefault(normalized, 0) + 1;
            duplicateCount.put(normalized, count);
            String uniqueKey = count == 1 ? keyBase : keyBase + "_" + count;

            ActivityRow item = new ActivityRow();
            item.activityName = activityName.trim();
            item.instrumentKey = uniqueKey;
            item.utKey = utKey.trim();
            item.evaluationPeriod = evaluation == null ? null : evaluation.intValue();
            item.weightPercent = scale2(totalWeight);
            item.raCodes = raCodes.stream().map(String::trim).distinct().toList();
            rows.add(item);
        }

        return rows;
    }

    private Map<String, Integer> buildEvaluationByUT(List<ActivityRow> rows) {
        Map<String, Integer> evalByUtKey = new LinkedHashMap<>();
        for (ActivityRow row : rows) {
            if (row.evaluationPeriod == null) {
                continue;
            }
            String key = normalize(row.utKey);
            Integer current = evalByUtKey.get(key);
            if (current == null) {
                evalByUtKey.put(key, row.evaluationPeriod);
            } else if (!current.equals(row.evaluationPeriod)) {
                throw new BusinessValidationException("UT " + row.utKey + " has mixed evaluation periods in activities");
            }
        }
        return evalByUtKey;
    }

    private Map<String, List<ExcelImportRequest.InstrumentItem>> buildInstrumentsByUT(List<ActivityRow> rows) {
        Map<String, List<ExcelImportRequest.InstrumentItem>> byUt = new LinkedHashMap<>();

        for (ActivityRow row : rows) {
            ExcelImportRequest.InstrumentItem instrumentItem = new ExcelImportRequest.InstrumentItem();
            instrumentItem.setKey(row.instrumentKey);
            instrumentItem.setName(row.activityName);
            instrumentItem.setWeightPercent(row.weightPercent);
            instrumentItem.setRaCodes(row.raCodes);

            byUt.computeIfAbsent(normalize(row.utKey), ignored -> new ArrayList<>()).add(instrumentItem);
        }

        return byUt;
    }

    private List<ExcelImportRequest.UTItem> parseUTs(Sheet datos,
                                                     Map<Integer, String> raCodeByColumn,
                                                     Map<String, Integer> evalByUtKey,
                                                     Map<String, List<ExcelImportRequest.InstrumentItem>> instrumentsByUtKey,
                                                     FormulaEvaluator evaluator,
                                                     DataFormatter formatter) {
        List<ExcelImportRequest.UTItem> uts = new ArrayList<>();

        for (int row = UT_ROW_START; row <= UT_ROW_END; row++) {
            String utKey = trimOrNull(getString(datos, row, COL_UT_KEY, evaluator, formatter));

            List<ExcelImportRequest.UTRADistributionItem> distributions = new ArrayList<>();
            for (int col = COL_RA_START; col <= COL_RA_END; col++) {
                BigDecimal percent = getDecimal(datos, row, col, evaluator, formatter);
                if (percent == null || percent.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                String raCode = trimOrNull(raCodeByColumn.get(col));
                if (isBlank(raCode)) {
                    continue;
                }

                ExcelImportRequest.UTRADistributionItem item = new ExcelImportRequest.UTRADistributionItem();
                item.setRaCode(raCode);
                item.setPercent(scale2(percent));
                distributions.add(item);
            }

            boolean hasData = !isBlank(utKey) || !distributions.isEmpty();
            if (!hasData) {
                continue;
            }

            if (isBlank(utKey)) {
                throw new BusinessValidationException("UT row " + (row + 1) + " has RA distribution but empty UT key");
            }
            if (distributions.isEmpty()) {
                throw new BusinessValidationException("UT " + utKey + " has no RA distribution > 0");
            }

            List<ExcelImportRequest.InstrumentItem> instruments =
                    instrumentsByUtKey.getOrDefault(normalize(utKey), List.of());
            if (instruments.isEmpty()) {
                throw new BusinessValidationException("UT " + utKey + " has no activities/instruments in table 3");
            }

            ExcelImportRequest.UTItem ut = new ExcelImportRequest.UTItem();
            ut.setKey(utKey);
            ut.setName(utKey);
            ut.setEvaluationPeriod(evalByUtKey.getOrDefault(normalize(utKey), 1));
            ut.setRaDistributions(distributions);
            ut.setInstruments(instruments);
            uts.add(ut);
        }

        return uts;
    }

    private List<ExcelImportRequest.StudentItem> parseStudents(Sheet datos,
                                                               Sheet actividades,
                                                               List<ActivityRow> activityRows,
                                                               FormulaEvaluator evaluator,
                                                               DataFormatter formatter) {
        List<ExcelImportRequest.StudentItem> students = new ArrayList<>();

        int emptyStreak = 0;
        for (int row = UT_ROW_START; row < 300; row++) {
            String name = trimOrNull(getString(datos, row, COL_STUDENT_NAME, evaluator, formatter));
            String code = trimOrNull(getString(datos, row, COL_STUDENT_CODE, evaluator, formatter));

            if (isBlank(name)) {
                emptyStreak++;
                if (emptyStreak >= 12) {
                    break;
                }
                continue;
            }
            emptyStreak = 0;

            if (isBlank(code)) {
                code = "S" + String.format("%03d", students.size() + 1);
            }

            ExcelImportRequest.StudentItem student = new ExcelImportRequest.StudentItem();
            student.setStudentCode(code);
            student.setFullName(name);
            student.setGrades(new ArrayList<>());
            students.add(student);
        }

        if (students.isEmpty()) {
            return students;
        }

        Map<String, String> instrumentKeyByActivityName = new LinkedHashMap<>();
        for (ActivityRow row : activityRows) {
            instrumentKeyByActivityName.put(normalize(row.activityName), row.instrumentKey);
        }

        Map<Integer, String> instrumentKeyByBlock = new HashMap<>();
        for (int i = 0; i < ACT_BLOCK_COUNT; i++) {
            int startCol = ACT_BLOCK_START_COL + (i * ACT_BLOCK_WIDTH);
            String blockName = trimOrNull(getString(actividades, 0, startCol, evaluator, formatter));
            if (isBlank(blockName)) {
                continue;
            }

            String key = instrumentKeyByActivityName.get(normalize(blockName));
            if (key == null) {
                key = blockName.trim();
            }
            instrumentKeyByBlock.put(i, key);
        }

        int maxRow = actividades.getLastRowNum();
        for (int row = ACT_ROW_STUDENT_START; row <= maxRow; row++) {
            String studentName = trimOrNull(getString(actividades, row, 1, evaluator, formatter)); // B
            if (isBlank(studentName)) {
                continue;
            }

            ExcelImportRequest.StudentItem student = students.stream()
                    .filter(s -> normalize(s.getFullName()).equals(normalize(studentName)))
                    .findFirst()
                    .orElse(null);
            if (student == null) {
                continue;
            }

            for (int i = 0; i < ACT_BLOCK_COUNT; i++) {
                String instrumentKey = instrumentKeyByBlock.get(i);
                if (isBlank(instrumentKey)) {
                    continue;
                }
                int startCol = ACT_BLOCK_START_COL + (i * ACT_BLOCK_WIDTH);

                BigDecimal grade = getDecimal(actividades, row, startCol, evaluator, formatter);
                if (grade == null) {
                    grade = computeGradeFromExercises(actividades, row, startCol + 1, evaluator, formatter);
                }
                if (grade == null) {
                    continue;
                }

                ExcelImportRequest.GradeItem item = new ExcelImportRequest.GradeItem();
                item.setInstrumentKey(instrumentKey);
                item.setGradeValue(scale2(grade));
                student.getGrades().add(item);
            }
        }

        return students;
    }

    private BigDecimal computeGradeFromExercises(Sheet actividades,
                                                 int studentRow,
                                                 int exerciseStartCol,
                                                 FormulaEvaluator evaluator,
                                                 DataFormatter formatter) {
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (int offset = 0; offset < 10; offset++) {
            int col = exerciseStartCol + offset;
            BigDecimal weight = getDecimal(actividades, ACT_ROW_WEIGHTS, col, evaluator, formatter);
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal grade = getDecimal(actividades, studentRow, col, evaluator, formatter);
            if (grade == null) {
                grade = BigDecimal.ZERO;
            }

            weighted = weighted.add(grade.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return weighted.divide(totalWeight, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal getDecimal(Sheet sheet,
                                  int rowIdx,
                                  int colIdx,
                                  FormulaEvaluator evaluator,
                                  DataFormatter formatter) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.FORMULA) {
                CellType evaluatedType = evaluator.evaluateFormulaCell(cell);
                if (evaluatedType == CellType.NUMERIC) {
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                }
                if (evaluatedType == CellType.STRING) {
                    return parseDecimal(formatter.formatCellValue(cell, evaluator));
                }
                return null;
            }

            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.STRING) {
                return parseDecimal(cell.getStringCellValue());
            }
        } catch (Exception ignored) {
            return null;
        }
        return parseDecimal(formatter.formatCellValue(cell, evaluator));
    }

    private String getString(Sheet sheet,
                             int rowIdx,
                             int colIdx,
                             FormulaEvaluator evaluator,
                             DataFormatter formatter) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell, evaluator);
        return trimOrNull(value);
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().replace("%", "").replace(",", ".");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static final class ActivityRow {
        private String activityName;
        private String instrumentKey;
        private String utKey;
        private Integer evaluationPeriod;
        private BigDecimal weightPercent;
        private List<String> raCodes;
    }
}
