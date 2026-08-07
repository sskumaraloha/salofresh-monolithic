package com.salofresh.entity;

import com.salofresh.audit.Auditable;
import com.salofresh.common.enums.ShiftSwapStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "shift_swap_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = {"salon", "requestingEmployee", "targetEmployee", "respondedBy"})
public class ShiftSwapRequest extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_employee_id", nullable = false)
    private Employee requestingEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_employee_id")
    private Employee targetEmployee;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "original_start_time", nullable = false)
    private LocalTime originalStartTime;

    @Column(name = "original_end_time", nullable = false)
    private LocalTime originalEndTime;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShiftSwapStatus status = ShiftSwapStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_id")
    private User respondedBy;

    @Column(name = "responded_at")
    private Instant respondedAt;
}
