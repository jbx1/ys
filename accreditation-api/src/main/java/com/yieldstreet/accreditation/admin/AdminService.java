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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    //todo: change this to minus days
    OffsetDateTime updateBeforeTs = OffsetDateTime.now().minusMinutes(expireConfirmedDays);

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

    User user = getOrCreateUser(createAccreditationRequest.getUserId());
    // todo: check that the user does not have another accreditation request PENDING

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
      Accreditation accreditation, FinalStatus status) {
    // todo: check that the accreditation is not already failed

    Accreditation.AccreditationStatus oldStatus = accreditation.getStatus();
    updateUsingStampedLock(accreditation, status);
    // we re-create the original request from the data so message is self-contained
    CreateAccreditationRequest request = recreateRequest(accreditation);
    kafkaProducer.notifyFinalise(
        request, accreditation.getId(), mapToStatus(status), mapToStatus(oldStatus));

    return new AccreditationResponse().accreditationId(accreditation.getId());
  }

  private void expireAccreditation(Accreditation accreditation) {
    Accreditation.AccreditationStatus oldStatus = accreditation.getStatus();
    logger.info(
        "Expiring {} which was last updated on {} to status {}",
        accreditation.getId(),
        accreditation.getUpdatedTs(),
        oldStatus);

    updateUsingStampedLock(accreditation, FinalStatus.EXPIRED);
    // we re-create the original request from the data so message is self-contained
    CreateAccreditationRequest request = recreateRequest(accreditation);
    kafkaProducer.notifyScheduledExpire(request, accreditation.getId(), mapToStatus(oldStatus));
  }

  private void updateUsingStampedLock(Accreditation accreditation, FinalStatus status) {
    OffsetDateTime updatedTs = accreditation.getUpdatedTs();
    logger.info(
        "Finalising Accreditation {} last updated on {} with status {}",
        accreditation.getId(),
        updatedTs,
        status);

    // we use the updatedTs timestamp as a stamped lock, to avoid pessimistic locking
    int updated =
        accreditationRepository.finaliseAccreditationStatus(
            mapToDatabaseStatus(status), accreditation.getId(), updatedTs);

    logger.debug("Updated {}", updated);
    if (updated == 0) {
      logger.warn(
          "No accreditation status update took place! Did someone else updated it in parallel?");
      throw new RuntimeException("Concurrent accreditation update aborted to avoid conflict.");
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
}
