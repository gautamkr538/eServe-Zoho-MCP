package com.zoho.eservemcp.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRecordsRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@eservecloud\\.in$", message = "Email must be from @eservecloud.in domain")
    private String email;
    
    @NotBlank(message = "From date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "From date must be in yyyy-MM-dd format")
    private String fromDate;
    
    @NotBlank(message = "To date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "To date must be in yyyy-MM-dd format")
    private String toDate;
    
    private String leaveStatus; // Optional: PENDING, APPROVED, REJECTED, ALL
}
