package com.holisun.backend.service.report;

import com.holisun.backend.dto.report.*;
import org.openpdf.text.*;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class ReportPdfExporter {

    private static final Font TITLE_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);

    private static final Font SECTION_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

    private static final Font HEADER_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

    private static final Font CELL_FONT =
            FontFactory.getFont(FontFactory.HELVETICA, 9);

    public byte[] exportOccupancy(OccupancyReportResponse report) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = createDocument();

        try {
            prepareDocument(document, output);

            addTitle(
                    document,
                    "Raport de ocupare",
                    report.from(),
                    report.to()
            );

            addSection(document, "Medici");

            PdfPTable doctors = createTable(
                    "ID",
                    "Nume",
                    "Minute ocupate",
                    "Minute disponibile",
                    "Rata ocupare"
            );

            for (ResourceOccupancyRow item : report.doctors()) {
                addCell(doctors, item.resourceId().toString());
                addCell(doctors, item.name());
                addCell(doctors, Long.toString(item.bookedMinutes()));
                addCell(doctors, Long.toString(item.availableMinutes()));
                addCell(doctors, percentage(item.occupancyRate()));
            }

            document.add(doctors);
            document.newPage();

            addSection(document, "Cabinete");

            PdfPTable rooms = createTable(
                    "ID",
                    "Nume",
                    "Minute ocupate",
                    "Minute disponibile",
                    "Rata ocupare"
            );

            for (ResourceOccupancyRow item : report.rooms()) {
                addCell(rooms, item.resourceId().toString());
                addCell(rooms, item.name());
                addCell(rooms, Long.toString(item.bookedMinutes()));
                addCell(rooms, Long.toString(item.availableMinutes()));
                addCell(rooms, percentage(item.occupancyRate()));
            }

            document.add(rooms);
            document.close();

            return output.toByteArray();

        } catch (DocumentException exception) {
            closeDocument(document);

            throw new IllegalStateException(
                    "PDF-ul pentru raportul de ocupare nu a putut fi generat.",
                    exception
            );
        }
    }

    public byte[] exportNoShow(NoShowReportResponse report) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = createDocument();

        try {
            prepareDocument(document, output);

            addTitle(
                    document,
                    "Raport no-show",
                    report.from(),
                    report.to()
            );

            document.add(new Paragraph(
                    "Total programari: " + report.total()
                            + " | Neprezentari: " + report.noShows()
                            + " | Rata: " + percentage(report.rate()),
                    CELL_FONT
            ));

            document.add(Chunk.NEWLINE);
            addSection(document, "Pacienti");

            PdfPTable patients = createTable(
                    "ID",
                    "Nume",
                    "Total programari",
                    "Neprezentari",
                    "Rata no-show"
            );

            for (PatientNoShowRow item : report.byPatient()) {
                addCell(patients, item.patientId().toString());
                addCell(patients, item.patientName());
                addCell(patients, Long.toString(item.total()));
                addCell(patients, Long.toString(item.noShows()));
                addCell(patients, percentage(item.rate()));
            }

            document.add(patients);
            document.newPage();

            addSection(document, "Zile");

            PdfPTable weekdays = createTable(
                    "Zi",
                    "Total programari",
                    "Neprezentari",
                    "Rata no-show"
            );

            for (WeekdayNoShowRow item : report.byWeekday()) {
                addCell(weekdays, item.dayOfWeek().name());
                addCell(weekdays, Long.toString(item.total()));
                addCell(weekdays, Long.toString(item.noShows()));
                addCell(weekdays, percentage(item.rate()));
            }

            document.add(weekdays);
            document.close();

            return output.toByteArray();

        } catch (DocumentException exception) {
            closeDocument(document);

            throw new IllegalStateException(
                    "PDF-ul pentru raportul no-show nu a putut fi generat.",
                    exception
            );
        }
    }

    public byte[] exportSales(SalesReportResponse report) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = createDocument();

        try {
            prepareDocument(document, output);

            addTitle(
                    document,
                    "Raport de vanzari",
                    report.from(),
                    report.to()
            );

            document.add(new Paragraph(
                    "Total general: " + money(report.grandTotal()),
                    SECTION_FONT
            ));

            document.add(Chunk.NEWLINE);
            addSection(document, "Servicii");

            PdfPTable services = createTable(
                    "ID",
                    "Nume",
                    "Programari finalizate",
                    "Total"
            );

            for (SalesRow item : report.byService()) {
                addCell(services, item.id().toString());
                addCell(services, item.name());
                addCell(
                        services,
                        Long.toString(item.appointments())
                );
                addCell(services, money(item.total()));
            }

            document.add(services);
            document.newPage();

            addSection(document, "Medici");

            PdfPTable doctors = createTable(
                    "ID",
                    "Nume",
                    "Programari finalizate",
                    "Total"
            );

            for (SalesRow item : report.byDoctor()) {
                addCell(doctors, item.id().toString());
                addCell(doctors, item.name());
                addCell(
                        doctors,
                        Long.toString(item.appointments())
                );
                addCell(doctors, money(item.total()));
            }

            document.add(doctors);
            document.close();

            return output.toByteArray();

        } catch (DocumentException exception) {
            closeDocument(document);

            throw new IllegalStateException(
                    "PDF-ul pentru raportul de vanzari nu a putut fi generat.",
                    exception
            );
        }
    }

    private Document createDocument() {
        return new Document(
                PageSize.A4.rotate(),
                36,
                36,
                54,
                54
        );
    }

    private void prepareDocument(
            Document document,
            ByteArrayOutputStream output
    ) throws DocumentException {
        PdfWriter writer = PdfWriter.getInstance(document, output);
        writer.setPageEvent(new PageNumberEvent());
        document.open();
    }

    private void addTitle(
            Document document,
            String reportName,
            LocalDate from,
            LocalDate to
    ) throws DocumentException {
        Paragraph clinic = new Paragraph("MedSched", TITLE_FONT);
        clinic.setAlignment(Element.ALIGN_CENTER);
        document.add(clinic);

        Paragraph title = new Paragraph(reportName, SECTION_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph(
                "Interval: " + from + " - " + to,
                CELL_FONT
        );
        period.setAlignment(Element.ALIGN_CENTER);
        document.add(period);

        Paragraph generated = new Paragraph(
                "Generat la: "
                        + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                ),
                CELL_FONT
        );
        generated.setAlignment(Element.ALIGN_CENTER);
        document.add(generated);

        document.add(Chunk.NEWLINE);
    }

    private void addSection(
            Document document,
            String name
    ) throws DocumentException {
        document.add(new Paragraph(name, SECTION_FONT));
        document.add(Chunk.NEWLINE);
    }

    private PdfPTable createTable(String... headers) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(12);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(header, HEADER_FONT)
            );

            cell.setPadding(6);
            table.addCell(cell);
        }

        return table;
    }

    private void addCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(
                new Phrase(value == null ? "" : value, CELL_FONT)
        );

        cell.setPadding(5);
        table.addCell(cell);
    }

    private String percentage(double value) {
        return String.format(
                Locale.ROOT,
                "%.1f%%",
                value * 100
        );
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }

        return String.format(
                Locale.ROOT,
                "%.2f",
                value
        );
    }

    private void closeDocument(Document document) {
        if (document.isOpen()) {
            document.close();
        }
    }

    private static class PageNumberEvent
            extends PdfPageEventHelper {

        @Override
        public void onEndPage(
                PdfWriter writer,
                Document document
        ) {
            Phrase footer = new Phrase(
                    "Pagina " + writer.getPageNumber(),
                    CELL_FONT
            );

            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    footer,
                    (document.left() + document.right()) / 2,
                    document.bottom() - 20,
                    0
            );
        }
    }
}