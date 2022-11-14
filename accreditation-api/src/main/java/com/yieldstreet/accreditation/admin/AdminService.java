package com.yieldstreet.accreditation.admin;

import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.accreditation.persistence.AccreditationRepository;
import com.yieldstreet.accreditation.persistence.User;
import com.yieldstreet.accreditation.persistence.UserRepository;
import com.yieldstreet.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final UserRepository userRepository;
    private final AccreditationRepository accreditationRepository;

    public AdminService(UserRepository userRepository, AccreditationRepository accreditationRepository) {
        this.userRepository = userRepository;
        this.accreditationRepository = accreditationRepository;
    }

    //todo: cron task to expire CONFIRMED after 30 days

    @Transactional
    public AccreditationResponse createAccreditation(CreateAccreditationRequest createAccreditationRequest) {

        User user = getOrCreateUser(createAccreditationRequest.getUserId());
        //todo: check that the user does not have another accreditation request PENDING

        Accreditation accreditation = new Accreditation();
        accreditation.setUser(user);
        accreditation.setType(mapAccreditationType(createAccreditationRequest.getAccreditationType()));
        accreditation.setStatus(Accreditation.AccreditationStatus.PENDING);

        Document document = createAccreditationRequest.getDocument();
        accreditation.setDocumentName(document.getName());
        accreditation.setDocumentMimeType(document.getMimeType());
        accreditation.setDocumentContent(document.getContent());

        accreditation = accreditationRepository.save(accreditation);

        //todo: send the Kafka message

        return new AccreditationResponse().accreditationId(accreditation.getId());
    }

    @Transactional
    public Optional<AccreditationResponse> finalizeAccreditation(UUID accreditationId, FinalStatus status) {
        return accreditationRepository.findById(accreditationId)
                .map(accreditation -> finalizeAccreditation(accreditation, status));

        //todo: send command to Kafka
    }

    private AccreditationResponse finalizeAccreditation(Accreditation accreditation, FinalStatus status) {
        //todo: check that the accreditation is not already failed
        OffsetDateTime updatedTs = accreditation.getUpdatedTs();
        accreditation.setStatus(mapAccreditationStatus(status));

        //we use the updatedTs timestamp as a stamped lock, to avoid pessimistic locking
        int updated = accreditationRepository.finaliseAccreditationStatus(mapAccreditationStatus(status), accreditation.getId(), updatedTs);
        if (updated != 1) {
            logger.warn("No accreditation status update took place! Did someone else updated it in parallel?");
            throw new RuntimeException("Concurrent accreditation update aborted to avoid conflict.");
        }

        return new AccreditationResponse().accreditationId(accreditation.getId());
    }

    private Accreditation.AccreditationType mapAccreditationType(AccreditationType accreditationType) {
        switch (accreditationType) {
            case INCOME:
                return Accreditation.AccreditationType.BY_INCOME;

            case NET_WORTH:
                return Accreditation.AccreditationType.BY_NET_WORTH;

            default:
                //shouldn't really happen, unless somehow a new one gets added to the REST API
                throw new RuntimeException("Unknown Accreditation Type: " + accreditationType);
        }
    }

    private Accreditation.AccreditationStatus mapAccreditationStatus(FinalStatus finalStatus) {
        switch (finalStatus) {
            case FAILED:
                return Accreditation.AccreditationStatus.FAILED;

            case CONFIRMED:
                return Accreditation.AccreditationStatus.CONFIRMED;

            case EXPIRED:
                return Accreditation.AccreditationStatus.EXPIRED;

            default:
                //shouldn't really happen, unless somehow a new one gets added to the REST API
                throw new RuntimeException("Unknown Accreditation Status: " + finalStatus);
        }
    }

    private User getOrCreateUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseGet(() -> createUser(userId));
    }

    private User createUser(String userId) {
        try {
            User user = new User();
            user.setUserId(userId);
            return userRepository.save(user);
        }
        catch (DataIntegrityViolationException ex) {
            logger.info("Seems the user is already there?", ex);
            logger.info("Trying to get it again.");
            return userRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Error creating new user."));
        }
    }

}
