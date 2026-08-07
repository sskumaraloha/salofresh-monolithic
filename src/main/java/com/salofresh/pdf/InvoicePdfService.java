package com.salofresh.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.salofresh.dto.payment.InvoiceResponse;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders the structured {@link InvoiceResponse} data (already produced/access-checked by
 * {@link com.salofresh.service.payment.PaymentService#getInvoice(Long, Long)}) into a downloadable
 * PDF document. This service performs no data lookups of its own - it is a pure rendering layer.
 */
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter PAID_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private static final Color BRAND_COLOR = new Color(33, 111, 90);
    private static final Color LIGHT_GREY = new Color(245, 245, 245);
    private static final Color MEDIUM_GREY = new Color(120, 120, 120);

    private static final Font FONT_BRAND = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BRAND_COLOR);
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
    private static final Font FONT_SECTION_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FONT_NORMAL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 9, MEDIUM_GREY);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONT_TOTAL_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FONT_GRAND_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND_COLOR);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, MEDIUM_GREY);

    /**
     * Builds a single-page, professional-looking PDF rendering of the given invoice data.
     *
     * @param invoice the already-authorized, fully-populated invoice data to render
     * @return the raw bytes of the generated PDF document
     * @throws RuntimeException if the underlying PDF layout engine fails to build the document
     */
    public byte[] generateInvoicePdf(InvoiceResponse invoice) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            addHeader(document, invoice);
            addPartiesSection(document, invoice);
            addLineItemsTable(document, invoice);
            addTotalsSection(document, invoice);
            addFooter(document);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate invoice PDF for invoice " + invoice.getInvoiceNumber(), e);
        }
        return outputStream.toByteArray();
    }

    private void addHeader(Document document, InvoiceResponse invoice) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1f, 1f});

        PdfPCell brandCell = borderlessCell(new Paragraph("SaloFresh", FONT_BRAND));
        brandCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        Paragraph titleParagraph = new Paragraph("TAX INVOICE", FONT_TITLE);
        titleParagraph.setAlignment(Element.ALIGN_RIGHT);
        PdfPCell titleCell = borderlessCell(titleParagraph);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        headerTable.addCell(brandCell);
        headerTable.addCell(titleCell);
        document.add(headerTable);

        document.add(new Paragraph(" ", FONT_SMALL));

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{1f, 1f});

        Paragraph invoiceNumberParagraph = new Paragraph();
        invoiceNumberParagraph.add(new Chunk("Invoice Number: ", FONT_NORMAL_BOLD));
        invoiceNumberParagraph.add(new Chunk(nullSafe(invoice.getInvoiceNumber()), FONT_NORMAL));
        metaTable.addCell(borderlessCell(invoiceNumberParagraph));

        Paragraph paidAtParagraph = new Paragraph();
        paidAtParagraph.setAlignment(Element.ALIGN_RIGHT);
        paidAtParagraph.add(new Chunk("Paid At: ", FONT_NORMAL_BOLD));
        paidAtParagraph.add(new Chunk(formatPaidAt(invoice), FONT_NORMAL));
        PdfPCell paidAtCell = borderlessCell(paidAtParagraph);
        paidAtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        metaTable.addCell(paidAtCell);

        document.add(metaTable);

        if (invoice.getBookingNumber() != null) {
            Paragraph bookingParagraph = new Paragraph();
            bookingParagraph.add(new Chunk("Booking Number: ", FONT_NORMAL_BOLD));
            bookingParagraph.add(new Chunk(invoice.getBookingNumber(), FONT_NORMAL));
            document.add(bookingParagraph);
        }

        document.add(new Paragraph(" ", FONT_SMALL));
        addHorizontalRule(document);
        document.add(new Paragraph(" ", FONT_SMALL));
    }

    private void addPartiesSection(Document document, InvoiceResponse invoice) throws DocumentException {
        PdfPTable partiesTable = new PdfPTable(2);
        partiesTable.setWidthPercentage(100);
        partiesTable.setWidths(new float[]{1f, 1f});
        partiesTable.setSpacingAfter(14f);

        Paragraph billedBy = new Paragraph();
        billedBy.add(new Chunk("BILLED BY\n", FONT_SECTION_HEADER));
        billedBy.add(new Chunk(nullSafe(invoice.getSalonName()), FONT_NORMAL));
        partiesTable.addCell(borderlessCell(billedBy));

        Paragraph billedTo = new Paragraph();
        billedTo.setAlignment(Element.ALIGN_RIGHT);
        billedTo.add(new Chunk("BILLED TO\n", FONT_SECTION_HEADER));
        billedTo.add(new Chunk(nullSafe(invoice.getCustomerName()), FONT_NORMAL));
        PdfPCell billedToCell = borderlessCell(billedTo);
        billedToCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        partiesTable.addCell(billedToCell);

        document.add(partiesTable);
    }

    private void addLineItemsTable(Document document, InvoiceResponse invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.4f, 1f, 1.3f, 1.3f});
        table.setSpacingBefore(4f);
        table.setSpacingAfter(10f);

        addTableHeaderCell(table, "Service");
        addTableHeaderCell(table, "Qty");
        addTableHeaderCell(table, "Unit Price");
        addTableHeaderCell(table, "Line Total");

        List<InvoiceResponse.LineItem> lineItems = invoice.getLineItems();
        if (lineItems == null || lineItems.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No line items available", FONT_NORMAL));
            emptyCell.setColspan(4);
            emptyCell.setPadding(6f);
            emptyCell.setBorderColor(LIGHT_GREY);
            table.addCell(emptyCell);
        } else {
            boolean shaded = false;
            for (InvoiceResponse.LineItem item : lineItems) {
                Color rowColor = shaded ? LIGHT_GREY : Color.WHITE;
                addBodyCell(table, nullSafe(item.getServiceName()), Element.ALIGN_LEFT, rowColor);
                addBodyCell(table, String.valueOf(item.getQuantity()), Element.ALIGN_CENTER, rowColor);
                addBodyCell(table, formatAmount(invoice, item.getUnitPrice()), Element.ALIGN_RIGHT, rowColor);
                addBodyCell(table, formatAmount(invoice, item.getLineTotal()), Element.ALIGN_RIGHT, rowColor);
                shaded = !shaded;
            }
        }

        document.add(table);
    }

    private void addTotalsSection(Document document, InvoiceResponse invoice) throws DocumentException {
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setWidths(new float[]{1f, 1f});

        addTotalRow(totalsTable, "Subtotal", formatAmount(invoice, invoice.getSubtotal()), FONT_TOTAL_LABEL, FONT_NORMAL);
        addTotalRow(totalsTable, "Discount", "-" + formatAmount(invoice, invoice.getDiscountAmount()), FONT_TOTAL_LABEL, FONT_NORMAL);
        addTotalRow(totalsTable, "Tax", formatAmount(invoice, invoice.getTaxAmount()), FONT_TOTAL_LABEL, FONT_NORMAL);

        PdfPCell separatorCell = new PdfPCell(new Phrase(" "));
        separatorCell.setColspan(2);
        separatorCell.setBorder(PdfPCell.TOP);
        separatorCell.setBorderColor(MEDIUM_GREY);
        separatorCell.setFixedHeight(4f);
        totalsTable.addCell(separatorCell);

        addTotalRow(totalsTable, "Total Paid", formatAmount(invoice, invoice.getTotalAmount()), FONT_GRAND_TOTAL, FONT_GRAND_TOTAL);

        document.add(totalsTable);

        document.add(new Paragraph(" ", FONT_SMALL));

        Paragraph paymentMethodParagraph = new Paragraph();
        paymentMethodParagraph.add(new Chunk("Payment Method: ", FONT_NORMAL_BOLD));
        paymentMethodParagraph.add(new Chunk(
                invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : "N/A", FONT_NORMAL));
        document.add(paymentMethodParagraph);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" ", FONT_SMALL));
        document.add(new Paragraph(" ", FONT_SMALL));
        addHorizontalRule(document);
        Paragraph footer = new Paragraph(
                "This is a computer-generated invoice issued by SaloFresh and does not require a signature. "
                        + "For any billing queries, please contact SaloFresh support.",
                FONT_FOOTER);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(6f);
        document.add(footer);
    }

    private void addHorizontalRule(Document document) throws DocumentException {
        PdfPTable ruleTable = new PdfPTable(1);
        ruleTable.setWidthPercentage(100);
        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setFixedHeight(1f);
        ruleCell.setBackgroundColor(MEDIUM_GREY);
        ruleCell.setBorder(PdfPCell.NO_BORDER);
        ruleTable.addCell(ruleCell);
        document.add(ruleTable);
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_HEADER));
        cell.setBackgroundColor(BRAND_COLOR);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int alignment, Color backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_NORMAL));
        cell.setBackgroundColor(backgroundColor);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(3f);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(3f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private PdfPCell borderlessCell(Paragraph content) {
        PdfPCell cell = new PdfPCell(content);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(2f);
        return cell;
    }

    private String formatPaidAt(InvoiceResponse invoice) {
        return invoice.getPaidAt() != null ? PAID_AT_FORMAT.format(invoice.getPaidAt()) : "N/A";
    }

    private String formatAmount(InvoiceResponse invoice, BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        String currency = invoice.getCurrency() != null ? invoice.getCurrency() : "";
        return (currency.isBlank() ? "" : currency + " ") + safeAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }
}
