package com.salofresh.dto.support;

import com.salofresh.common.enums.TicketCategory;
import com.salofresh.common.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    @NotNull(message = "Category is required")
    private TicketCategory category;

    /**
     * Optional; defaults to {@link TicketPriority#MEDIUM} when not supplied.
     */
    private TicketPriority priority;
}
