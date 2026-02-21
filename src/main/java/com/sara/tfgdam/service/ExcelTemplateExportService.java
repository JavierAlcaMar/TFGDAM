package com.sara.tfgdam.service;

import com.sara.tfgdam.repository.ActivityRepository;
import com.sara.tfgdam.repository.CourseModuleRepository;
import com.sara.tfgdam.repository.InstrumentRepository;
import com.sara.tfgdam.repository.LearningOutcomeRARepository;
import com.sara.tfgdam.repository.TeachingUnitUTRepository;
import com.sara.tfgdam.exception.BusinessValidationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class ExcelTemplateExportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelTemplateExportService.class);

    private static final String TEMPLATE_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int RA_ROW_START = 8; // row 9 (0-based index)
    private static final int RA_MAX = 10;
    private static final int UT_ROW_START = 8; // row 9 (0-based index)
    private static final int UT_MAX = 20;
    private static final int ACT_ROW_START = 34; // row 35 (0-based index)
    private static final int ACT_MAX = 30;

    @Value("${sara.export.python-command:${SARA_EXPORT_PYTHON_COMMAND:python}}")
    private String pythonCommand;

    @Value("${sara.export.script-path:${SARA_EXPORT_SCRIPT_PATH:./scripts/export_backend_template.py}}")
    private String exportScriptPath;

    @Value("${sara.export.template-path:${SARA_EXPORT_TEMPLATE_PATH:./storage/templates/source_template_unprotected.xlsx}}")
    private String templatePath;

    @Value("${sara.export.filled-template-path:${SARA_EXPORT_FILLED_TEMPLATE_PATH:./storage/templates/source_template_rellenada_unprotected.xlsx}}")
    private String filledTemplatePath;

    @Value("${sara.export.timeout-seconds:${SARA_EXPORT_TIMEOUT_SECONDS:120}}")
    private int timeoutSeconds;

    @Value("${sara.export.use-import-snapshot:${SARA_EXPORT_USE_IMPORT_SNAPSHOT:false}}")
    private boolean useImportSnapshot;

    private final ExcelTemplateSnapshotService excelTemplateSnapshotService;
    private final CourseModuleRepository courseModuleRepository;
    private final LearningOutcomeRARepository learningOutcomeRARepository;
    private final TeachingUnitUTRepository teachingUnitUTRepository;
    private final ActivityRepository activityRepository;
    private final InstrumentRepository instrumentRepository;

    public ExcelTemplateExportService(ExcelTemplateSnapshotService excelTemplateSnapshotService,
                                      CourseModuleRepository courseModuleRepository,
                                      LearningOutcomeRARepository learningOutcomeRARepository,
                                      TeachingUnitUTRepository teachingUnitUTRepository,
                                      ActivityRepository activityRepository,
                                      InstrumentRepository instrumentRepository) {
        this.excelTemplateSnapshotService = excelTemplateSnapshotService;
        this.courseModuleRepository = courseModuleRepository;
        this.learningOutcomeRARepository = learningOutcomeRARepository;
        this.teachingUnitUTRepository = teachingUnitUTRepository;
        this.activityRepository = activityRepository;
        this.instrumentRepository = instrumentRepository;
    }

    public ExportedExcel exportModuleExcel(Long moduleId, String authorizationHeader, String baseUrl) {
        String token = extractBearerToken(authorizationHeader);

        Path script = Paths.get(exportScriptPath).toAbsolutePath().normalize();
        if (!Files.exists(script)) {
            throw new BusinessValidationException("Export script not found: " + script);
        }

        Path template = resolveTemplatePath(moduleId);
        if (!Files.exists(template)) {
            throw new BusinessValidationException("Export template not found: " + template);
        }

        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("sara-module-export-" + moduleId + "-", ".xlsx");

            List<String> command = new ArrayList<>();
            command.add(pythonCommand);
            command.add(script.toString());
            command.add("--base-url");
            command.add(baseUrl);
            command.add("--token");
            command.add(token);
            command.add("--module-id");
            command.add(String.valueOf(moduleId));
            command.add("--template");
            command.add(template.toString());
            command.add("--output");
            command.add(outputFile.toString());

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessValidationException(
                        "Excel export timed out after " + timeoutSeconds + " seconds"
                );
            }

            String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new BusinessValidationException(
                        "Excel export failed" + (processOutput.isEmpty() ? "" : ": " + processOutput)
                );
            }

            byte[] content = Files.readAllBytes(outputFile);
            String filename = "module-" + moduleId + "-export.xlsx";
            return new ExportedExcel(filename, TEMPLATE_MIME_TYPE, content);
        } catch (IOException ex) {
            throw new BusinessValidationException("Cannot export Excel template: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessValidationException("Excel export interrupted");
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                    // Ignore temp cleanup failures.
                }
            }
        }
    }

    public ExportedExcel exportBaseTemplateExcel() {
        Path template = Paths.get(templatePath).toAbsolutePath().normalize();
        if (!Files.exists(template)) {
            throw new BusinessValidationException("Base export template not found: " + template);
        }

        try {
            byte[] content = Files.readAllBytes(template);
            String filename = template.getFileName() == null
                    ? "source_template_unprotected.xlsx"
                    : template.getFileName().toString();
            return new ExportedExcel(filename, TEMPLATE_MIME_TYPE, content);
        } catch (IOException ex) {
            throw new BusinessValidationException("Cannot export base template: " + ex.getMessage());
        }
    }

    public ExportedExcel exportFilledTemplateExcel() {
        Path template = Paths.get(filledTemplatePath).toAbsolutePath().normalize();
        if (!Files.exists(template)) {
            throw new BusinessValidationException("Filled export template not found: " + template);
        }

        try {
            byte[] content = Files.readAllBytes(template);
            String filename = template.getFileName() == null
                    ? "source_template_rellenada_unprotected.xlsx"
                    : template.getFileName().toString();
            return new ExportedExcel(filename, TEMPLATE_MIME_TYPE, content);
        } catch (IOException ex) {
            throw new BusinessValidationException("Cannot export filled template: " + ex.getMessage());
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new BusinessValidationException("Authorization Bearer token is required for export");
        }

        String trimmed = authorizationHeader.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            String token = trimmed.substring(7).trim();
            if (token.isEmpty()) {
                throw new BusinessValidationException("Authorization Bearer token is required for export");
            }
            return token;
        }
        return trimmed;
    }

    private Path resolveTemplatePath(Long moduleId) {
        if (useImportSnapshot) {
            Path snapshot = excelTemplateSnapshotService.resolveSnapshotPath(moduleId);
            if (snapshot != null && Files.exists(snapshot) && isSnapshotCompatible(moduleId, snapshot)) {
                return snapshot.toAbsolutePath().normalize();
            }
        }
        return Paths.get(templatePath).toAbsolutePath().normalize();
    }

    private boolean isSnapshotCompatible(Long moduleId, Path snapshotPath) {
        var moduleOpt = courseModuleRepository.findById(moduleId);
        if (moduleOpt.isEmpty()) {
            return false;
        }

        String moduleName = normalize(moduleOpt.get().getName());
        if (moduleName.isEmpty()) {
            return false;
        }

        try (InputStream inputStream = Files.newInputStream(snapshotPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet datos = workbook.getSheet("Datos Iniciales");
            if (datos == null) {
                return false;
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            String snapshotModuleName = "";
            if (datos.getRow(2) != null && datos.getRow(2).getCell(1) != null) {
                snapshotModuleName = formatter.formatCellValue(datos.getRow(2).getCell(1));
            }

            boolean matches = normalize(snapshotModuleName).equals(moduleName);
            if (!matches) {
                log.warn(
                        "Ignoring snapshot {} for module {}: snapshot module name '{}' does not match current module name '{}'",
                        snapshotPath,
                        moduleId,
                        snapshotModuleName,
                        moduleOpt.get().getName()
                );
                return false;
            }

            SnapshotSummary snapshotSummary = readSnapshotSummary(datos, formatter);
            int raCount = learningOutcomeRARepository.findByModuleId(moduleId).size();
            int utCount = teachingUnitUTRepository.findByModuleId(moduleId).size();
            var activities = activityRepository.findByModuleId(moduleId);
            int instrumentCount = activities.isEmpty()
                    ? 0
                    : instrumentRepository.findByActivityIdIn(activities.stream().map(item -> item.getId()).toList()).size();

            if (snapshotSummary.raCount != raCount
                    || snapshotSummary.utCount != utCount
                    || snapshotSummary.instrumentCount != instrumentCount) {
                log.warn(
                        "Ignoring snapshot {} for module {} due structure mismatch (snapshot: RAs={}, UTs={}, instruments={}; current: RAs={}, UTs={}, instruments={})",
                        snapshotPath,
                        moduleId,
                        snapshotSummary.raCount,
                        snapshotSummary.utCount,
                        snapshotSummary.instrumentCount,
                        raCount,
                        utCount,
                        instrumentCount
                );
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Cannot validate snapshot {} for module {}: {}", snapshotPath, moduleId, ex.getMessage());
            return false;
        }
    }

    private SnapshotSummary readSnapshotSummary(Sheet datos, DataFormatter formatter) {
        int raCount = 0;
        for (int offset = 0; offset < RA_MAX; offset++) {
            int rowIndex = RA_ROW_START + offset;
            BigDecimal weight = asDecimal(getCell(datos, rowIndex, 2), formatter);
            if (weight.compareTo(BigDecimal.ZERO) > 0) {
                raCount++;
            }
        }

        int utCount = 0;
        for (int offset = 0; offset < UT_MAX; offset++) {
            int rowIndex = UT_ROW_START + offset;
            BigDecimal total = BigDecimal.ZERO;
            for (int col = 5; col <= 14; col++) {
                total = total.add(asDecimal(getCell(datos, rowIndex, col), formatter));
            }
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                utCount++;
            }
        }

        int instrumentCount = 0;
        for (int offset = 0; offset < ACT_MAX; offset++) {
            int rowIndex = ACT_ROW_START + offset;
            String utKey = normalize(formatCell(getCell(datos, rowIndex, 15), formatter));
            if (utKey.isEmpty()) {
                continue;
            }

            BigDecimal total = BigDecimal.ZERO;
            for (int col = 5; col <= 14; col++) {
                total = total.add(asDecimal(getCell(datos, rowIndex, col), formatter));
            }
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                instrumentCount++;
            }
        }

        return new SnapshotSummary(raCount, utCount, instrumentCount);
    }

    private Cell getCell(Sheet sheet, int rowIndex, int colIndex) {
        if (sheet == null || sheet.getRow(rowIndex) == null) {
            return null;
        }
        return sheet.getRow(rowIndex).getCell(colIndex);
    }

    private String formatCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell);
    }

    private BigDecimal asDecimal(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            String raw = formatter.formatCellValue(cell);
            if (raw == null) {
                return BigDecimal.ZERO;
            }
            String normalized = raw.trim().replace("%", "").replace(",", ".");
            if (normalized.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(normalized);
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record SnapshotSummary(int raCount, int utCount, int instrumentCount) {
    }

    public record ExportedExcel(String filename, String contentType, byte[] content) {
    }
}
