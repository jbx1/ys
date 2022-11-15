package com.yieldstreet.accreditation.admin;

import com.yieldstreet.accreditation.audit.KafkaProducer;
import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.accreditation.persistence.AccreditationRepository;
import com.yieldstreet.accreditation.persistence.User;
import com.yieldstreet.accreditation.persistence.UserRepository;
import com.yieldstreet.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.yieldstreet.accreditation.mappers.AccreditationTypeMapper.mapAccreditationType;
import static com.yieldstreet.accreditation.mappers.AccreditationTypeMapper.mapPersistedAccreditationType;
import static com.yieldstreet.accreditation.mappers.StatusMapper.mapToDatabaseStatus;
import static com.yieldstreet.accreditation.mappers.StatusMapper.mapToStatus;

@Service
public class AdminService {

  private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

  /** The repositories used to store the data * */
  private final UserRepository userRepository;

  private final AccreditationRepository accreditationRepository;

  private final KafkaProducer kafkaProducer;

  @Value("${yieldstreet.accreditations.expire_confirmed_days:30}")
  private int expireConfirmedDays;

  public AdminService(
      UserRepository userRepository,
      AccreditationRepository accreditationRepository,
      KafkaProducer kafkaProducer) {
    this.userRepository = userRepository;
    this.accreditationRepository = accreditationRepository;
    this.kafkaProducer = kafkaProducer;
  }

  @Transactional
  public void expireOldConfirmedAccreditations() {
    OffsetDateTime updateBeforeTs = OffsetDateTime.now().minusDays(expireConfirmedDays);

    List<Accreditation> oldConfirmedAccreditations =
        accreditationRepository.findAccreditationByStatusAndUpdatedTsBefore(
            Accreditation.AccreditationStatus.CONFIRMED, updateBeforeTs);
    if (!oldConfirmedAccreditations.isEmpty()) {
      logger.info(
          "Expiring {} confirmed accreditations older than {}",
          oldConfirmedAccreditations.size(),
          updateBeforeTs);

      oldConfirmedAccreditations.forEach(this::expireAccreditation);
    }
  }

  @Transactional
  public AccreditationResponse createAccreditation(
      CreateAccreditationRequest createAccreditationRequest) {

    String userId = createAccreditationRequest.getUserId();
    List<Accreditation> pendingRequests = accreditationRepository.findByUserUserIdAndStatus(userId, Accreditation.AccreditationStatus.PENDING);
    if (!pendingRequests.isEmpty()) {
      String msg = String.format("User %s already has %d PENDING request(s).", userId, pendingRequests.size());
      logger.warn(msg);
      throw new InvalidOperationException(msg);
    }

    User user = getOrCreateUser(userId);

    Accreditation accreditation = new Accreditation();
    accreditation.setUser(user);
    accreditation.setType(mapAccreditationType(createAccreditationRequest.getAccreditationType()));
    accreditation.setStatus(Accreditation.AccreditationStatus.PENDING);

    Document document = createAccreditationRequest.getDocument();
    accreditation.setDocumentName(document.getName());
    accreditation.setDocumentMimeType(document.getMimeType());
    accreditation.setDocumentContent(document.getContent());

    accreditation = accreditationRepository.save(accreditation);

    kafkaProducer.notifyCreate(createAccreditationRequest, accreditation.getId(), Status.PENDING);

    return new AccreditationResponse().accreditationId(accreditation.getId());
  }

  @Transactional
  public Optional<AccreditationResponse> finalizeAccreditation(
      UUID accreditationId, FinalStatus status) {
    return accreditationRepository
        .findById(accreditationId)
        .map(accreditation -> finalizeAccreditation(accreditation, status));
  }

  private AccreditationResponse finalizeAccreditation(
      Accreditation accreditation, FinalStatus finalStatus) {

    Accreditation.AccreditationStatus newStatus = mapToDatabaseStatus(finalStatus);
    Accreditation.AccreditationStatus oldStatus = accreditation.getStatus();

    if (newStatus.equals(oldStatus)) {
      //make the API call idempotent (PUT should be), just return the same result
      logger.info("Idempotent finalisation of {} with status {} when it was already {}", accreditation.getId(), newStatus, oldStatus);
      return new AccreditationResponse().accreditationId(accreditation.getId());
    }

    if (accreditation.getStatus().equals(Accreditation.AccreditationStatus.FAILED)) {
      String msg = String.format("Accreditation request %s is already in FAILED status. Cannot update further.", accreditation.getId());
      logger.warn(msg);
      throw new InvalidOperationException(accreditation.getId() + " is already in FAILED status.");
    }

    updateUsingStampedLock(accreditation, newStatus);
    // we re-create the original request from the data so message is self-contained
    CreateAccreditationRequest request = recreateRequest(accreditation);
    kafkaProducer.notifyFinalise(
        request, accreditation.getId(), mapToStatus(finalStatus), mapToStatus(oldStatus));

    return new AccreditationResponse().accreditationId(accreditation.getId());
  }

  private void expireAccreditation(Accreditation accreditation) {
    Accreditation.AccreditationStatus oldStatus = accreditation.getStatus();
    logger.info(
        "Expiring {} which was last updated on {} to status {}",
        accreditation.getId(),
        accreditation.getUpdatedTs(),
        oldStatus);

    updateUsingStampedLock(accreditation, mapToDatabaseStatus(FinalStatus.EXPIRED));
    // we re-create the original request from the data so message is self-contained
    CreateAccreditationRequest request = recreateRequest(accreditation);
    kafkaProducer.notifyScheduledExpire(request, accreditation.getId(), mapToStatus(oldStatus));
  }

  private void updateUsingStampedLock(Accreditation accreditation, Accreditation.AccreditationStatus status) {
    OffsetDateTime updatedTs = accreditation.getUpdatedTs();
    logger.info(
        "Finalising Accreditation {} last updated on {} with status {}",
        accreditation.getId(),
        updatedTs,
        status);

    // we use the updatedTs timestamp as a stamped lock, to avoid pessimistic locking
    int updated =
        accreditationRepository.finaliseAccreditationStatus(status, accreditation.getId(), updatedTs);

    logger.debug("Updated {}", updated);
    if (updated == 0) {
      logger.warn(
          "No accreditation status update took place! Did someone else updated it in parallel?");
      throw new InvalidOperationException("Concurrent accreditation update aborted to avoid conflict.");
    } else if (updated > 1) {
      logger.warn("Somehow more than one record was updated (which should be impossible).");
    }
  }

  private User getOrCreateUser(String userId) {
    return userRepository.findByUserId(userId).orElseGet(() -> createUser(userId));
  }

  private User createUser(String userId) {
    try {
      User user = new User();
      user.setUserId(userId);
      return userRepository.save(user);
    } catch (DataIntegrityViolationException ex) {
      logger.info("Seems the user is already there?", ex);
      logger.info("Trying to get it again.");
      return userRepository
          .findByUserId(userId)
          .orElseThrow(() -> new RuntimeException("Error creating new user."));
    }
  }

  private CreateAccreditationRequest recreateRequest(Accreditation accreditation) {
    CreateAccreditationRequest createAccreditationRequest = new CreateAccreditationRequest();
    createAccreditationRequest.setUserId(accreditation.getUser().getUserId());
    createAccreditationRequest.setAccreditationType(
        mapPersistedAccreditationType(accreditation.getType()));
    Document document = new Document();
    document.setName(accreditation.getDocumentName());
    document.setContent(accreditation.getDocumentContent());
    document.setMimeType(accreditation.getDocumentMimeType());
    createAccreditationRequest.setDocument(document);
    return createAccreditationRequest;
  }

  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid Operation")
  public static class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String reason) {
      super(reason);
    }
  }
}
