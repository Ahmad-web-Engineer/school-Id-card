package com.platform.idcard.order.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.order.dto.*;
import com.platform.idcard.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Orders", description = "Admin order verification, review, corrections, and quotation generation")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List all orders", description = "Paginated list of all school orders with filters")
    public ResponseEntity<PagedResponse<AdminOrderSummaryResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(orderService.getAdminOrders(page, size, schoolId, search, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details", description = "Retrieves full order details including student snapshots and quotation")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAdminOrderById(id)));
    }

    @PostMapping("/{id}/start-review")
    @Operation(summary = "Start order review", description = "Transitions order from SUBMITTED to UNDER_REVIEW")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> startReview(@PathVariable UUID id) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.startReview(id, adminUserId)));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify order", description = "Verifies student data and transitions order to VERIFIED")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> verifyOrder(@PathVariable UUID id) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.verifyOrder(id, adminUserId)));
    }

    @PostMapping("/{id}/corrections")
    @Operation(summary = "Request correction", description = "Requests data/photo corrections and transitions order to CORRECTION_REQUIRED")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> requestCorrection(
            @PathVariable UUID id,
            @Valid @RequestBody RequestCorrectionRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.requestCorrection(id, adminUserId, request)));
    }

    @PostMapping("/{id}/quotation")
    @Operation(summary = "Generate quotation", description = "Generates commercial quotation with unit pricing and tax")
    public ResponseEntity<ApiResponse<QuotationResponse>> generateQuotation(
            @PathVariable UUID id,
            @Valid @RequestBody GenerateQuotationRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        QuotationResponse response = orderService.generateQuotation(id, adminUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/print-pdf")
    @Operation(summary = "Download print-ready order cards PDF", description = "Generates high-resolution multi-page PDF with all student cards in the order")
    public ResponseEntity<byte[]> downloadOrderPrintPdf(@PathVariable UUID id) {
        byte[] pdfBytes = orderService.renderOrderCardsPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"order_cards_" + id + ".pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/{id}/students/{studentId}/print-pdf")
    @Operation(summary = "Download single student ID card PDF", description = "Generates single student print-ready ID card PDF")
    public ResponseEntity<byte[]> downloadOrderStudentPrintPdf(@PathVariable UUID id, @PathVariable UUID studentId) {
        byte[] pdfBytes = orderService.renderOrderSingleStudentPdf(id, studentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"card_" + studentId + ".pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/{id}/print-a4-sheet")
    @Operation(summary = "Download A4 imposition sheet PDF", description = "Generates multi-page A4 imposition sheet PDF with duplex column mirroring for production printing")
    public ResponseEntity<byte[]> downloadOrderA4SheetPdf(@PathVariable UUID id) {
        byte[] pdfBytes = orderService.renderOrderA4SheetPdf(id);
        String filename = "order_" + id + "_a4_sheet.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    @GetMapping("/{id}/print-a4-image")
    @Operation(summary = "Download A4 imposition sheet JPEG", description = "Generates high-resolution 300 DPI A4 imposition sheet JPEG image with crop marks and duplex layout")
    public ResponseEntity<byte[]> downloadOrderA4SheetImage(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "front") String side
    ) {
        byte[] imageBytes = orderService.renderOrderA4SheetImage(id, side);
        String filename = "order_" + id + "_a4_" + (side != null ? side.toLowerCase() : "front") + ".jpg";
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(imageBytes);
    }

    @GetMapping("/{id}/download-images")
    @Operation(summary = "Download high-resolution JPEG images ZIP", description = "Exports 300 DPI JPEG cards in a zip archive")
    public ResponseEntity<byte[]> downloadOrderImagesZip(@PathVariable UUID id) {
        byte[] zipBytes = orderService.renderOrderImagesZip(id);
        String filename = "order_" + id + "_cards_jpeg.zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(zipBytes);
    }

    @GetMapping("/{id}/students/{studentId}/download-image")
    @Operation(summary = "Download single student card JPEG", description = "Exports single student card side as 300 DPI JPEG")
    public ResponseEntity<byte[]> downloadOrderStudentImage(
            @PathVariable UUID id,
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "front") String side
    ) {
        byte[] imageBytes = orderService.renderOrderSingleStudentImage(id, studentId, side);
        String filename = "card_" + studentId + "_" + (side != null ? side.toLowerCase() : "front") + ".jpg";
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(imageBytes);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close order", description = "Formally closes an order once fulfillment and verification are complete")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> closeOrder(@PathVariable UUID id) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.closeOrder(id, adminUserId)));
    }
}
