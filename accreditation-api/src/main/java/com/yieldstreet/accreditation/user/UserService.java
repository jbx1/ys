package com.yieldstreet.accreditation.user;

import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.accreditation.persistence.AccreditationRepository;
import com.yieldstreet.accreditation.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Service
public class UserService {

  private static final Logger logger = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  private final AccreditationRepository accreditationRepository;

  public UserService(
      UserRepository userRepository, AccreditationRepository accreditationRepository) {
    this.userRepository = userRepository;
    this.accreditationRepository = accreditationRepository;
  }

  public List<Accreditation> getUserAccreditations(String userId) {
    logger.info("Getting accreditations for user {}", userId);
    List<Accreditation> accreditations = accreditationRepository.findByUserUserIdOrderByCreatedTs(userId);

    logger.info("Got {} entries", accreditations.size());
    if (accreditations.isEmpty() && userRepository.findByUserId(userId).isEmpty()) {
      throw new UserNotFound();
    }

    return accreditations;
  }

  @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "User not found")
  public static class UserNotFound extends RuntimeException {
    public UserNotFound() {
      super("User not found.");
    }
  }
}
