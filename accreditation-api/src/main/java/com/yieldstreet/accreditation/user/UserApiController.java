package com.yieldstreet.accreditation.user;

import com.yieldstreet.accreditation.mappers.AccreditationMapper;
import com.yieldstreet.accreditation.mappers.AccreditationTypeMapper;
import com.yieldstreet.accreditation.mappers.StatusMapper;
import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.api.UserApi;
import com.yieldstreet.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserApiController implements UserApi {

  private static final Logger logger = LoggerFactory.getLogger(UserApiController.class);

  private final UserService userService;

  public UserApiController(UserService userService) {
    this.userService = userService;
  }

  @Override
  public ResponseEntity<AccreditationStatusResponse> getAccreditationStatuses(String userId) {
    logger.info("Getting accreditations for user {}", userId);
    List<Accreditation> userAccreditations = userService.getUserAccreditations(userId);

    AccreditationStatusResponse response = new AccreditationStatusResponse();
    response.setUserId(userId);

    Map<String, AccreditationStatusDetails> accreditations =
        userAccreditations.stream()
            .collect(
                Collectors.toMap(
                    accreditation -> accreditation.getId().toString(), AccreditationMapper::mapToStatusDetails));

    response.setAccreditationStatuses(accreditations);

    return ResponseEntity.ok(response);
  }
}
