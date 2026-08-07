package com.salofresh.controller.payment;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payment.InvoiceResponse;
import com.salofresh.pdf.InvoicePdfService;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves a downloadable PDF rendering of a payment's invoice. Kept separate from
 * {@link PaymentController} which serves the structured JSON invoice representation.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Downloadable PDF invoices for payments")
public class InvoicePdfController {

    private final PaymentService paymentService;
    private final InvoicePdfService invoicePdfService;
    private final SecurityUtils securityUtils;

    @GetMapping("/{id}/invoice.pdf")
    @Operation(summary = "Download a PDF invoice for a successful payment")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        InvoiceResponse invoice = paymentService.getInvoice(id, securityUtils.getCurrentUserId());
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoice);

        String invoiceNumber = invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : String.valueOf(id);
        String filename = "invoice-" + invoiceNumber + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
