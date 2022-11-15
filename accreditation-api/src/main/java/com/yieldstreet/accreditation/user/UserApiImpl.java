package com.yieldstreet.accreditation.user;

import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.api.UserApi;
import com.yieldstreet.model.AccreditationStatus;
import com.yieldstreet.model.AccreditationStatusResponse;
import com.yieldstreet.model.AccreditationType;
import com.yieldstreet.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserApiImpl implements UserApi {

  private static final Logger logger = LoggerFactory.getLogger(UserApiImpl.class);

  private final UserService userService;

  public UserApiImpl(UserService userService) {
    this.userService = userService;
  }

  @Override
  public ResponseEntity<AccreditationStatusResponse> getAccreditationStatuses(String userId) {
    logger.info("Getting accreditations for user {}", userId);
    List<Accreditation> userAccreditations = userService.getUserAccreditations(userId);

    AccreditationStatusResponse response = new AccreditationStatusResponse();
    response.setUserId(userId);

    Map<String, AccreditationStatus> accreditations =
        userAccreditations.stream()
            .collect(
                Collectors.toMap(
                    accreditation -> accreditation.getId().toString(), this::mapToStatus));

    response.setAccreditationStatuses(accreditations);

    return ResponseEntity.ok(response);
  }

  private AccreditationStatus mapToStatus(Accreditation accreditation) {
    AccreditationStatus accreditationStatus = new AccreditationStatus();
    switch (accreditation.getStatus()) {
      case FAILED:
        accreditationStatus.setStatus(Status.FAILED);
        break;

      case CONFIRMED:
        accreditationStatus.setStatus(Status.CONFIRMED);
        break;

      case EXPIRED:
        accreditationStatus.setStatus(Status.EXPIRED);
        break;

      default:
        accreditationStatus.setStatus(Status.PENDING);
        break;
    }

    switch (accreditation.getType()) {
      case BY_INCOME:
        accreditationStatus.setAccreditationType(AccreditationType.INCOME);
        break;

      case BY_NET_WORTH:
        accreditationStatus.setAccreditationType(AccreditationType.NET_WORTH);
        break;
    }

    return accreditationStatus;
  }
}
