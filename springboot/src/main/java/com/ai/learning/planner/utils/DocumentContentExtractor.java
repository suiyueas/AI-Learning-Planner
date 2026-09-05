package com.ai.learning.planner.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;

/**
 * 知识文档内容提取器
 * 按文件扩展名分发解析：md/txt 走 UTF-8 文本，pdf 走 PDFBox，
 * docx/doc 走 POI Word，pptx 走 POI 幻灯片，xlsx 走 POI 表格；
 * 输出统一为纯文本，供 DocumentChunker 分块与向量化入库
 */
@Component
@Slf4j
public class DocumentContentExtractor {

    /** 单文件解析上限（字符），防止超大文档拖垮分块与向量化 */
    private static final int MAX_CHARS = 2_000_000;

    /**
     * 提取文件纯文本内容
     *
     * @param file 文件路径（解析失败返回 null，由调用方降级处理）
     */
    public String extract(Path file) {
        if (file == null || !Files.exists(file) || !Files.isReadable(file)) {
            log.warn("文件不存在或不可读: {}", file);
            return null;
        }

        String fileName = file.getFileName() != null ? file.getFileName().toString() : "";
        String ext = getExtension(fileName);

        String content;
        try {
            content = switch (ext) {
                case "txt", "md", "markdown" -> extractPlainText(file);
                case "pdf" -> extractPdf(file);
                case "docx" -> extractDocx(file);
                case "doc" -> extractDoc(file);
                case "pptx" -> extractPptx(file);
                case "ppt" -> extractPpt(file);
                case "xlsx" -> extractXlsx(file);
                case "xls" -> extractXls(file);
                default -> {
                    log.warn("不支持的文档格式: {}，尝试按纯文本读取", ext);
                    yield extractPlainText(file);
                }
            };
        } catch (Exception e) {
            log.error("文档解析失败: file={}, ext={}, error={}", fileName, ext, e.getMessage());
            return null;
        }

        if (content == null) {
            return null;
        }

        content = normalize(content);
        if (content.length() > MAX_CHARS) {
            log.warn("文档内容超过上限，截断: file={}, chars={}→{}", fileName, content.length(), MAX_CHARS);
            content = content.substring(0, MAX_CHARS);
        }
        return content;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 纯文本读取（UTF-8，兼容 BOM） */
    private String extractPlainText(Path file) throws Exception {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        return content;
    }

    /** PDF 文本提取（PDFBox 3.x） */
    private String extractPdf(Path file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /** DOCX 文本提取（按文档 body 顺序遍历段落与表格） */
    private String extractDocx(Path file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
             XWPFDocument doc = new XWPFDocument(in)) {

            for (var element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        sb.append(text).append('\n');
                    }
                } else if (element instanceof XWPFTable table) {
                    appendTable(sb, table);
                }
            }
        }
        return sb.toString();
    }

    private void appendTable(StringBuilder sb, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            StringBuilder line = new StringBuilder();
            for (XWPFTableCell cell : row.getTableCells()) {
                if (!line.isEmpty()) {
                    line.append(" | ");
                }
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    StringBuilder cellText = new StringBuilder();
                    for (XWPFRun run : paragraph.getRuns()) {
                        cellText.append(run.getText(0) == null ? "" : run.getText(0));
                    }
                    line.append(cellText);
                }
            }
            if (!line.isEmpty()) {
                sb.append(line).append('\n');
            }
        }
    }

    /** DOC 旧格式文本提取（POI HWPF） */
    private String extractDoc(Path file) throws Exception {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
             HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    /** PPTX 文本提取（逐页提取文本框与表格文本） */
    private String extractPptx(Path file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
             XMLSlideShow ppt = new XMLSlideShow(in)) {

            int slideNo = 0;
            for (XSLFSlide slide : ppt.getSlides()) {
                slideNo++;
                sb.append("[幻灯片 ").append(slideNo).append("]\n");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append('\n');
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    /** PPT 旧格式文本提取（POI HSLF） */
    private String extractPpt(Path file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
             HSLFSlideShow ppt = new HSLFSlideShow(in)) {

            int slideNo = 0;
            for (HSLFSlide slide : ppt.getSlides()) {
                slideNo++;
                sb.append("[幻灯片 ").append(slideNo).append("]\n");
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append('\n');
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    /** XLS 旧格式文本提取（POI HSSF） */
    private String extractXls(Path file) throws Exception {
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);

        try (InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
             HSSFWorkbook workbook = new HSSFWorkbook(in)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("[工作表: ").append(sheet.getSheetName()).append("]\n");
                for (Iterator<Row> rowIt = sheet.rowIterator(); rowIt.hasNext(); ) {
                    Row row = rowIt.next();
                    StringBuilder line = new StringBuilder();
                    for (Iterator<Cell> cellIt = row.cellIterator(); cellIt.hasNext(); ) {
                        Cell cell = cellIt.next();
                        if (!line.isEmpty()) line.append(" | ");
                        line.append(formatCell(formatter, cell));
                    }
                    if (!line.isEmpty()) sb.append(line).append('\n');
                }
            }
        }
        return sb.toString();
    }

    /** XLSX 文本提取（逐工作表逐行输出，数字与日期格式化为可读文本） */
    private String extractXlsx(Path file) throws Exception {
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);

        try (InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
             Workbook workbook = new XSSFWorkbook(in)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("[工作表: ").append(sheet.getSheetName()).append("]\n");
                for (Iterator<Row> rowIt = sheet.rowIterator(); rowIt.hasNext(); ) {
                    Row row = rowIt.next();
                    StringBuilder line = new StringBuilder();
                    for (Iterator<Cell> cellIt = row.cellIterator(); cellIt.hasNext(); ) {
                        Cell cell = cellIt.next();
                        if (!line.isEmpty()) {
                            line.append(" | ");
                        }
                        line.append(formatCell(formatter, cell));
                    }
                    if (!line.isEmpty()) {
                        sb.append(line).append('\n');
                    }
                }
            }
        }
        return sb.toString();
    }

    private String formatCell(DataFormatter formatter, Cell cell) {
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
            return cell.getCellFormula();
        }
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return formatter.formatCellValue(cell);
    }

    /** 规整文本：去除 \r、压缩连续空行，便于分块 */
    private String normalize(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replaceAll("(?m)^[ \\t]+$", "");
        normalized = normalized.replaceAll("\n{3,}", "\n\n");
        return normalized.strip();
    }
}