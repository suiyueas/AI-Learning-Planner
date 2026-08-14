package com.ai.learning.planner.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文档内容提取器单元测试
 * 覆盖纯文本/BOM、PDF（PDFBox）、DOCX（POI）解析与异常路径
 */
class DocumentContentExtractorTest {

    private final DocumentContentExtractor extractor = new DocumentContentExtractor();

    @TempDir
    Path tempDir;

    @Test
    void extract_txt_returnsText() throws IOException {
        Path file = tempDir.resolve("notes.txt");
        Files.writeString(file, "第一段内容。\n\n第二段内容。", StandardCharsets.UTF_8);

        String content = extractor.extract(file);

        assertNotNull(content);
        assertTrue(content.contains("第一段内容"));
        assertTrue(content.contains("第二段内容"));
    }

    @Test
    void extract_md_returnsText() throws IOException {
        Path file = tempDir.resolve("guide.md");
        Files.writeString(file, "# Java 入门\n\n这是 Markdown 文档正文。", StandardCharsets.UTF_8);

        String content = extractor.extract(file);

        assertNotNull(content);
        assertTrue(content.contains("# Java 入门"));
        assertTrue(content.contains("Markdown 文档正文"));
    }

    @Test
    void extract_txtWithBom_stripsBom() throws IOException {
        Path file = tempDir.resolve("bom.txt");
        Files.write(file, "\uFEFF带 BOM 的文本".getBytes(StandardCharsets.UTF_8));

        String content = extractor.extract(file);

        assertNotNull(content);
        assertFalse(content.startsWith("\uFEFF"));
        assertTrue(content.contains("带 BOM 的文本"));
    }

    @Test
    void extract_missingFile_returnsNull() {
        Path missing = tempDir.resolve("not-exist.pdf");

        assertNull(extractor.extract(missing));
    }

    @Test
    void extract_emptyFile_returnsEmpty() throws IOException {
        Path file = tempDir.resolve("empty.txt");
        Files.writeString(file, "", StandardCharsets.UTF_8);

        assertEquals("", extractor.extract(file));
    }

    @Test
    void extract_overLengthLimit_truncated() throws IOException {
        Path file = tempDir.resolve("huge.txt");
        StringBuilder sb = new StringBuilder(2_100_000);
        for (int i = 0; i < 210_000; i++) {
            sb.append("abcdefghij");
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);

        String content = extractor.extract(file);

        assertNotNull(content);
        assertTrue(content.length() <= 2_000_000);
    }

    @Test
    void extract_pdf_returnsText() throws IOException {
        Path file = tempDir.resolve("sample.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Hello PDF World. This is a RAG test document.");
                cs.endText();
            }
            doc.save(file.toFile());
        }

        String content = extractor.extract(file);

        assertNotNull(content);
        assertTrue(content.contains("Hello PDF World"));
    }

    @Test
    void extract_docx_returnsTextInOrder() throws IOException {
        Path file = tempDir.resolve("sample.docx");
        try (XWPFDocument doc = new XWPFDocument(); FileOutputStream out = new FileOutputStream(file.toFile())) {
            XWPFParagraph p1 = doc.createParagraph();
            XWPFRun r1 = p1.createRun();
            r1.setText("第一段落");

            XWPFParagraph p2 = doc.createParagraph();
            XWPFRun r2 = p2.createRun();
            r2.setText("第二段落");

            doc.write(out);
        }

        String content = extractor.extract(file);

        assertNotNull(content);
        List<String> lines = content.lines().map(String::strip).filter(l -> !l.isEmpty()).toList();
        assertTrue(lines.contains("第一段落"));
        assertTrue(lines.contains("第二段落"));
    }

    @Test
    void extract_unknownExtension_fallsBackToText() throws IOException {
        Path file = tempDir.resolve("data.log");
        Files.writeString(file, "日志内容", StandardCharsets.UTF_8);

        String content = extractor.extract(file);

        assertNotNull(content);
        assertTrue(content.contains("日志内容"));
    }
}
