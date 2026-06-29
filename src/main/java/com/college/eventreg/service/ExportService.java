package com.college.eventreg.service;

import com.college.eventreg.model.EventRegistration;

// Explicit imports to avoid conflicts with Apache POI
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Font;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    public ByteArrayInputStream exportToCsv(List<EventRegistration> registrations) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            // Write UTF-8 BOM so Excel opens it with correct formatting
            writer.write('\ufeff');
            writer.println("Registration ID,Student Name,Email,Register Number,Department,Event Name,Status,Date");

            for (EventRegistration r : registrations) {
                writer.println(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        r.getId(),
                        r.getName().replace("\"", "\"\""),
                        r.getStudent() != null ? r.getStudent().getUsername().replace("\"", "\"\"") : "",
                        r.getRegisterNumber().replace("\"", "\"\""),
                        r.getDepartment().replace("\"", "\"\""),
                        r.getEventName().replace("\"", "\"\""),
                        r.getStatus(),
                        r.getRegistrationDate() != null ? r.getRegistrationDate().toString() : ""));
            }
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream exportToExcel(List<EventRegistration> registrations) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Registrations");

            // Header font and style
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Row headers
            String[] columns = { "ID", "Student Name", "Email", "Register Number", "Department", "Event Name", "Status",
                    "Date" };
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            for (EventRegistration r : registrations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(r.getName());
                row.createCell(2).setCellValue(r.getStudent() != null ? r.getStudent().getUsername() : "");
                row.createCell(3).setCellValue(r.getRegisterNumber());
                row.createCell(4).setCellValue(r.getDepartment());
                row.createCell(5).setCellValue(r.getEventName());
                row.createCell(6).setCellValue(r.getStatus().toString());
                row.createCell(7)
                        .setCellValue(r.getRegistrationDate() != null ? r.getRegistrationDate().toString() : "");
            }

            // Auto size columns
            for (int col = 0; col < columns.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportToPdf(List<EventRegistration> registrations) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Event Registrations Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Table setup
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new int[] { 1, 3, 2, 2, 3, 2, 2 });

            // Table headers (using java.awt.Color.WHITE)
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            String[] headers = { "ID", "Student Name", "Reg No", "Dept", "Event", "Status", "Date" };

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
                cell.setBackgroundColor(new java.awt.Color(0, 114, 255)); // Blue color matching our theme
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (EventRegistration r : registrations) {
                PdfPCell cell;

                cell = new PdfPCell(new Phrase(String.valueOf(r.getId()), cellFont));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(r.getName(), cellFont));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPaddingLeft(5);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(r.getRegisterNumber(), cellFont));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(r.getDepartment(), cellFont));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(r.getEventName(), cellFont));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPaddingLeft(5);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(String.valueOf(r.getStatus()), cellFont));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);

                cell = new PdfPCell(new Phrase(
                        r.getRegistrationDate() != null ? r.getRegistrationDate().toLocalDate().toString() : "",
                        cellFont));
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            document.add(table);
            document.close();

        } catch (DocumentException ex) {
            logger.error("Error occurred while generating PDF: {}", ex.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateEventPass(EventRegistration r) {
        Document document = new Document(PageSize.A6, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, java.awt.Color.WHITE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, java.awt.Color.GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.BLACK);

            // Header
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            PdfPCell headerCell = new PdfPCell(new Phrase("EVENT ENTRY PASS", headerFont));
            headerCell.setBackgroundColor(new java.awt.Color(139, 92, 246)); // Purple color to match our theme
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPadding(8);
            headerCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(headerCell);
            document.add(headerTable);

            // Spacer
            Paragraph spacer = new Paragraph(" ");
            spacer.setSpacingAfter(10);
            document.add(spacer);

            // Details Table
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new int[] { 1, 1 });

            // Event Name (Colspan 2)
            PdfPCell eventCell = new PdfPCell();
            eventCell.setColspan(2);
            eventCell.setBorder(Rectangle.BOTTOM);
            eventCell.setBorderColor(new java.awt.Color(226, 232, 240));
            eventCell.setPaddingBottom(6);
            eventCell.addElement(new Phrase("EVENT NAME", labelFont));
            eventCell.addElement(new Phrase(r.getEventName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, java.awt.Color.BLACK)));
            detailsTable.addCell(eventCell);

            // Attendee Info
            PdfPCell studentCell = new PdfPCell();
            studentCell.setBorder(Rectangle.NO_BORDER);
            studentCell.setPaddingTop(6);
            studentCell.addElement(new Phrase("ATTENDEE", labelFont));
            studentCell.addElement(new Phrase(r.getName(), valueFont));
            detailsTable.addCell(studentCell);

            PdfPCell regNoCell = new PdfPCell();
            regNoCell.setBorder(Rectangle.NO_BORDER);
            regNoCell.setPaddingTop(6);
            regNoCell.addElement(new Phrase("REGISTER NO", labelFont));
            regNoCell.addElement(new Phrase(r.getRegisterNumber(), valueFont));
            detailsTable.addCell(regNoCell);

            // Venue and Date
            PdfPCell venueCell = new PdfPCell();
            venueCell.setBorder(Rectangle.NO_BORDER);
            venueCell.setPaddingTop(6);
            venueCell.addElement(new Phrase("VENUE", labelFont));
            venueCell.addElement(new Phrase(r.getEvent() != null ? r.getEvent().getVenue() : "Main Campus", valueFont));
            detailsTable.addCell(venueCell);

            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setPaddingTop(6);
            dateCell.addElement(new Phrase("DATE / TIME", labelFont));
            dateCell.addElement(new Phrase(
                    r.getEvent() != null ? r.getEvent().getEventDate().toString().replace("T", " ") : "", valueFont));
            detailsTable.addCell(dateCell);

            document.add(detailsTable);
            document.add(spacer);

            // Generate QR Code — include a visual status symbol in the scanned text
            boolean isConfirmed = r.getStatus() == com.college.eventreg.model.RegistrationStatus.CONFIRMED;
            String statusLine = isConfirmed ? "CONFIRMED" : "WAITLISTED";
            String verificationData = String.format(
                    "--------------------------------\n" +
                            "REG ID  : %d\n" +
                            "STUDENT : %s\n" +
                            "REG NO  : %s\n" +
                            "EVENT   : %s\n" +
                            "STATUS  : %s\n" +
                            "--------------------------------",
                    r.getId(), r.getName(), r.getRegisterNumber(), r.getEventName(), statusLine);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(verificationData, BarcodeFormat.QR_CODE, 120, 120);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] qrBytes = pngOutputStream.toByteArray();

            // Add QR Code
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            qrImage.scaleAbsolute(90, 90);
            document.add(qrImage);

            // Footer
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, java.awt.Color.GRAY);
            Paragraph footer = new Paragraph("Show this QR Code at the entry gate for scanning verification.",
                    footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            document.add(footer);

            document.close();
        } catch (Exception ex) {
            logger.error("Error occurred while generating Event Pass PDF: {}", ex.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
