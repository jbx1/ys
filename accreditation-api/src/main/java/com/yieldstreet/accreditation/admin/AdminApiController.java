package com.yieldstreet.accreditation.admin;

import com.yieldstreet.api.AdminApi;
import com.yieldstreet.model.AccreditationResponse;
import com.yieldstreet.model.CreateAccreditationRequest;
import com.yieldstreet.model.FinaliseAccreditationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AdminApiController implements AdminApi {

  private static final Logger logger = LoggerFactory.getLogger(AdminApiController.class);

  private final AdminService adminService;

  public AdminApiController(AdminService adminService) {
    this.adminService = adminService;
  }

  @Override
  public ResponseEntity<AccreditationResponse> createAccreditation(
      CreateAccreditationRequest request) {

    logger.info(
        "Received Accreditation Request for user {} of type {} with document {}",
        request.getUserId(),
        request.getAccreditationType(),
        request.getDocument().getName());

    try {
      AccreditationResponse response = adminService.createAccreditation(request);
      return ResponseEntity.ok(response);
    } catch (AdminService.AdminServiceError ex) {
      throw ex;
    } catch (Exception ex) {
      logger.error("Unable to perform operation", ex);
      // let's avoid leaking internal errors to the front-end
      throw new UnableToPerformOperationException(
          "Unable to perform the requested operation. Please refer to the logs for more details.");
    }
  }

  @Override
  public ResponseEntity<AccreditationResponse> finalizeAccreditation(
      UUID accreditationId, FinaliseAccreditationRequest request) {
    logger.info(
        "Received Finalize Accreditation Request for accreditation ID {} with outcome status {}",
        accreditationId,
        request.getOutcome());

    try {
      return adminService
          .finalizeAccreditation(accreditationId, request.getOutcome())
          .map(ResponseEntity::ok)
          .orElseGet(() -> ResponseEntity.notFound().build());
    } catch (AdminService.AdminServiceError ex) {
      throw ex;
    } catch (Exception ex) {
      logger.error("Unable to perform operation", ex);
      // let's avoid leaking internal errors to the front-end
      throw new UnableToPerformOperationException(
          "Unable to perform the requested operation. Please refer to the logs for more details.");
    }
  }

  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Unable to perform operation")
  public static class UnableToPerformOperationException extends RuntimeException {
    public UnableToPerformOperationException(String reason) {
      super(reason);
    }
  }
}
