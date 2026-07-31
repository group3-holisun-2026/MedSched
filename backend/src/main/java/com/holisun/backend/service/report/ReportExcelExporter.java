package com.holisun.backend.service.report;

import com.holisun.backend.dto.report.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

@Component
public class ReportExcelExporter {

    public byte[] exportOccupancy(OccupancyReportResponse report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Styles styles = createStyles(workbook);

            Sheet doctors = workbook.createSheet("Medici");
            addPeriod(doctors, report.from(), report.to(), styles);

            Row doctorHeader = createHeader(
                    doctors,
                    3,
                    styles,
                    "ID",
                    "Nume",
                    "Minute ocupate",
                    "Minute disponibile",
                    "Rata ocupare"
            );

            int rowIndex = doctorHeader.getRowNum() + 1;

            for (ResourceOccupancyRow item : report.doctors()) {
                Row row = doctors.createRow(rowIndex++);

                row.createCell(0).setCellValue(item.resourceId().toString());
                row.createCell(1).setCellValue(item.name());
                row.createCell(2).setCellValue(item.bookedMinutes());
                row.createCell(3).setCellValue(item.availableMinutes());

                Cell rateCell = row.createCell(4);
                rateCell.setCellValue(item.occupancyRate());
                rateCell.setCellStyle(styles.percent());
            }

            finishSheet(doctors, 5);

            Sheet rooms = workbook.createSheet("Cabinete");
            addPeriod(rooms, report.from(), report.to(), styles);

            Row roomHeader = createHeader(
                    rooms,
                    3,
                    styles,
                    "ID",
                    "Nume",
                    "Minute ocupate",
                    "Minute disponibile",
                    "Rata ocupare"
            );

            rowIndex = roomHeader.getRowNum() + 1;

            for (ResourceOccupancyRow item : report.rooms()) {
                Row row = rooms.createRow(rowIndex++);

                row.createCell(0).setCellValue(item.resourceId().toString());
                row.createCell(1).setCellValue(item.name());
                row.createCell(2).setCellValue(item.bookedMinutes());
                row.createCell(3).setCellValue(item.availableMinutes());

                Cell rateCell = row.createCell(4);
                rateCell.setCellValue(item.occupancyRate());
                rateCell.setCellStyle(styles.percent());
            }

            finishSheet(rooms, 5);

            workbook.write(output);
            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Fisierul Excel pentru ocupare nu a putut fi generat.",
                    exception
            );
        }
    }

    public byte[] exportNoShow(NoShowReportResponse report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Styles styles = createStyles(workbook);

            Sheet patients = workbook.createSheet("Pacienti");
            addPeriod(patients, report.from(), report.to(), styles);

            Row patientHeader = createHeader(
                    patients,
                    3,
                    styles,
                    "ID",
                    "Nume",
                    "Total programari",
                    "Neprezentari",
                    "Rata no-show"
            );

            int rowIndex = patientHeader.getRowNum() + 1;

            for (PatientNoShowRow item : report.byPatient()) {
                Row row = patients.createRow(rowIndex++);

                row.createCell(0).setCellValue(item.patientId().toString());
                row.createCell(1).setCellValue(item.patientName());
                row.createCell(2).setCellValue(item.total());
                row.createCell(3).setCellValue(item.noShows());

                Cell rateCell = row.createCell(4);
                rateCell.setCellValue(item.rate());
                rateCell.setCellStyle(styles.percent());
            }

            finishSheet(patients, 5);

            Sheet weekdays = workbook.createSheet("Zile");
            addPeriod(weekdays, report.from(), report.to(), styles);

            Row weekdayHeader = createHeader(
                    weekdays,
                    3,
                    styles,
                    "Zi",
                    "Total programari",
                    "Neprezentari",
                    "Rata no-show"
            );

            rowIndex = weekdayHeader.getRowNum() + 1;

            for (WeekdayNoShowRow item : report.byWeekday()) {
                Row row = weekdays.createRow(rowIndex++);

                row.createCell(0).setCellValue(item.dayOfWeek().name());
                row.createCell(1).setCellValue(item.total());
                row.createCell(2).setCellValue(item.noShows());

                Cell rateCell = row.createCell(3);
                rateCell.setCellValue(item.rate());
                rateCell.setCellStyle(styles.percent());
            }

            finishSheet(weekdays, 4);

            workbook.write(output);
            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Fisierul Excel pentru no-show nu a putut fi generat.",
                    exception
            );
        }
    }

    public byte[] exportSales(SalesReportResponse report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Styles styles = createStyles(workbook);

            Sheet services = workbook.createSheet("Servicii");
            addPeriod(services, report.from(), report.to(), styles);

            Row serviceHeader = createHeader(
                    services,
                    3,
                    styles,
                    "ID",
                    "Nume",
                    "Programari finalizate",
                    "Total"
            );

            int rowIndex = serviceHeader.getRowNum() + 1;

            for (SalesRow item : report.byService()) {
                Row row = services.createRow(rowIndex++);

                row.createCell(0).setCellValue(item.id().toString());
                row.createCell(1).setCellValue(item.name());
                row.createCell(2).setCellValue(item.appointments());

                Cell totalCell = row.createCell(3);
                totalCell.setCellValue(item.total().doubleValue());
                totalCell.setCellStyle(styles.money());
            }

            finishSheet(services, 4);

            Sheet doctors = workbook.createSheet("Medici");
            addPeriod(doctors, report.from(), report.to(), styles);

            Row doctorHeader = createHeader(
                    doctors,
                    3,
                    styles,
                    "ID",
                    "Nume",
                    "Programari finalizate",
                    "Total"
            );

            rowIndex = doctorHeader.getRowNum() + 1;

            for (SalesRow item : report.byDoctor()) {
                Row row = doctors.createRow(rowIndex++);

                row.createCell(0).setCellValue(item.id().toString());
                row.createCell(1).setCellValue(item.name());
                row.createCell(2).setCellValue(item.appointments());

                Cell totalCell = row.createCell(3);
                totalCell.setCellValue(item.total().doubleValue());
                totalCell.setCellStyle(styles.money());
            }

            finishSheet(doctors, 4);

            workbook.write(output);
            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Fisierul Excel pentru vanzari nu a putut fi generat.",
                    exception
            );
        }
    }

    private void addPeriod(
            Sheet sheet,
            LocalDate from,
            LocalDate to,
            Styles styles
    ) {
        Row fromRow = sheet.createRow(0);
        fromRow.createCell(0).setCellValue("De la");

        Cell fromCell = fromRow.createCell(1);
        fromCell.setCellValue(Date.valueOf(from));
        fromCell.setCellStyle(styles.date());

        Row toRow = sheet.createRow(1);
        toRow.createCell(0).setCellValue("Pana la");

        Cell toCell = toRow.createCell(1);
        toCell.setCellValue(Date.valueOf(to));
        toCell.setCellStyle(styles.date());
    }

    private Row createHeader(
            Sheet sheet,
            int rowIndex,
            Styles styles,
            String... titles
    ) {
        Row row = sheet.createRow(rowIndex);

        for (int index = 0; index < titles.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(titles[index]);
            cell.setCellStyle(styles.header());
        }

        return row;
    }

    private void finishSheet(Sheet sheet, int numberOfColumns) {
        sheet.createFreezePane(0, 4);

        for (int index = 0; index < numberOfColumns; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private Styles createStyles(XSSFWorkbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle header = workbook.createCellStyle();
        header.setFont(boldFont);

        CellStyle percent = workbook.createCellStyle();
        percent.setDataFormat(
                workbook.createDataFormat().getFormat("0.0%")
        );

        CellStyle money = workbook.createCellStyle();
        money.setDataFormat(
                workbook.createDataFormat().getFormat("#,##0.00")
        );

        CellStyle date = workbook.createCellStyle();
        date.setDataFormat(
                workbook.createDataFormat().getFormat("dd.mm.yyyy")
        );

        return new Styles(header, percent, money, date);
    }

    private record Styles(
            CellStyle header,
            CellStyle percent,
            CellStyle money,
            CellStyle date
    ) {
    }
}