package com.salofresh.controller.calendar;

import com.salofresh.calendar.IcsCalendarService;
import com.salofresh.constant.AppConstants;
import com.salofresh.entity.Appointment;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/appointments/{id}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Calendar Sync", description = "Export a booked appointment as an .ics download or a Google Calendar link")
public class CalendarController {

    private final AppointmentRepository appointmentRepository;
    private final IcsCalendarService icsCalendarService;
    private final SecurityUtils securityUtils;

    @GetMapping("/calendar.ics")
    @Operation(summary = "Download the appointment as an .ics calendar file")
    public ResponseEntity<byte[]> downloadIcs(@PathVariable Long id) {
        Appointment appointment = getOwnedAppointment(id);
        byte[] body = icsCalendarService.buildIcsForAppointment(appointment).getBytes(StandardCharsets.UTF_8);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("booking-" + appointment.getBookingNumber() + ".ics", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(body);
    }

    @GetMapping("/calendar/google-link")
    @Operation(summary = "Get a pre-filled Google Calendar 'add event' link for the appointment")
    public ResponseEntity<ApiResponse<String>> googleCalendarLink(@PathVariable Long id) {
        Appointment appointment = getOwnedAppointment(id);
        String link = icsCalendarService.buildGoogleCalendarLink(appointment);
        return ResponseEntity.ok(ApiResponse.success("Google Calendar link generated successfully", link));
    }

    private Appointment getOwnedAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isOwner = appointment.getCustomer() != null && appointment.getCustomer().getId().equals(currentUserId);
        if (!isOwner && !isAdmin()) {
            throw new ForbiddenException("You do not have permission to access this appointment's calendar export");
        }
        return appointment;
    }

    private boolean isAdmin() {
        return securityUtils.getCurrentUser().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
