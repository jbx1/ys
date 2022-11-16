package com.yieldstreet.accreditation.admin;

import com.yieldstreet.accreditation.MockHelpers;
import com.yieldstreet.accreditation.audit.KafkaProducer;
import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.accreditation.persistence.AccreditationRepository;
import com.yieldstreet.accreditation.persistence.User;
import com.yieldstreet.accreditation.persistence.UserRepository;
import com.yieldstreet.model.AccreditationType;
import com.yieldstreet.model.CreateAccreditationRequest;
import com.yieldstreet.model.Document;
import com.yieldstreet.model.FinalStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AdminServiceTest {
  @Mock UserRepository mockUserRepository;

  @Mock AccreditationRepository mockAccreditationRepository;

  @Mock KafkaProducer mockKafkaProducer;

  private AutoCloseable closeable;

  private AdminService adminService;

  @BeforeEach
  public void setup() {
    closeable = MockitoAnnotations.openMocks(this);

    adminService =
        new AdminService(mockUserRepository, mockAccreditationRepository, mockKafkaProducer);
  }

  @AfterEach
  void shutdown() throws Exception {
    closeable.close();
  }

  @Test
  void disallowNewRequestsIfPreviousOnesArePending() {

    String userId = "123456";
    when(mockAccreditationRepository.findByUserUserIdAndStatus(
            userId, Accreditation.AccreditationStatus.PENDING))
        .thenReturn(MockHelpers.mockAccreditations(userId, 1));

    CreateAccreditationRequest request = new CreateAccreditationRequest();
    request.setUserId(userId);
    request.setAccreditationType(AccreditationType.INCOME);
    request.setDocument(
        new Document()
            .name("2018.pdf")
            .mimeType("application/pdf")
            .content("952109582109582gkdsgsdg"));

    assertThrows(
        AdminService.UserAlreadyPendingAccreditationError.class,
        () -> adminService.createAccreditation(request));
  }

  @Test
  void disallowFailedFromBeingUpdated() {
    UUID uuid = UUID.randomUUID();
    User user = MockHelpers.mockUser("133214");
    Accreditation accreditation =
        MockHelpers.mockAccreditation(uuid, user, Accreditation.AccreditationStatus.FAILED);

    when(mockAccreditationRepository.findById(uuid)).thenReturn(Optional.of(accreditation));

    assertThrows(
        AdminService.AccreditationAlreadyInFailedStateError.class,
        () -> adminService.finalizeAccreditation(uuid, FinalStatus.CONFIRMED));
  }

  @Test
  void allowIdempotentFailedUpdate() {
    UUID uuid = UUID.randomUUID();
    User user = MockHelpers.mockUser("133214");
    Accreditation accreditation =
        MockHelpers.mockAccreditation(uuid, user, Accreditation.AccreditationStatus.FAILED);

    when(mockAccreditationRepository.findById(uuid)).thenReturn(Optional.of(accreditation));

    assertTrue(adminService.finalizeAccreditation(uuid, FinalStatus.FAILED).isPresent());
  }


  @Test
  void allowPendingToBeConfirmed() {
    UUID uuid = UUID.randomUUID();
    User user = MockHelpers.mockUser("133214");
    Accreditation accreditation =
        MockHelpers.mockAccreditation(uuid, user, Accreditation.AccreditationStatus.PENDING);

    when(mockAccreditationRepository.findById(uuid)).thenReturn(Optional.of(accreditation));
    when(mockAccreditationRepository.finaliseAccreditationStatus(
            Accreditation.AccreditationStatus.CONFIRMED, uuid, accreditation.getUpdatedTs()))
        .thenReturn(1);

    assertTrue(adminService.finalizeAccreditation(uuid, FinalStatus.CONFIRMED).isPresent());
  }

  @Test
  void allowPendingToBeFailed() {
    UUID uuid = UUID.randomUUID();
    User user = MockHelpers.mockUser("133214");
    Accreditation accreditation =
            MockHelpers.mockAccreditation(uuid, user, Accreditation.AccreditationStatus.PENDING);

    when(mockAccreditationRepository.findById(uuid)).thenReturn(Optional.of(accreditation));
    when(mockAccreditationRepository.finaliseAccreditationStatus(
            Accreditation.AccreditationStatus.FAILED, uuid, accreditation.getUpdatedTs()))
            .thenReturn(1);

    assertTrue(adminService.finalizeAccreditation(uuid, FinalStatus.FAILED).isPresent());
  }

  @Test
  void allowConfirmedToExpire() {
    UUID uuid = UUID.randomUUID();
    User user = MockHelpers.mockUser("133214");
    Accreditation accreditation =
            MockHelpers.mockAccreditation(uuid, user, Accreditation.AccreditationStatus.CONFIRMED);

    when(mockAccreditationRepository.findById(uuid)).thenReturn(Optional.of(accreditation));
    when(mockAccreditationRepository.finaliseAccreditationStatus(
            Accreditation.AccreditationStatus.EXPIRED, uuid, accreditation.getUpdatedTs()))
            .thenReturn(1);

    assertTrue(adminService.finalizeAccreditation(uuid, FinalStatus.EXPIRED).isPresent());
  }

  @Test
  void stampedLockPreventsUpdate() {
    UUID uuid = UUID.randomUUID();
    User user = MockHelpers.mockUser("133214");
    Accreditation accreditation =
        MockHelpers.mockAccreditation(uuid, user, Accreditation.AccreditationStatus.CONFIRMED);

    when(mockAccreditationRepository.findById(uuid)).thenReturn(Optional.of(accreditation));
    when(mockAccreditationRepository.finaliseAccreditationStatus(
            Accreditation.AccreditationStatus.EXPIRED, uuid, accreditation.getUpdatedTs()))
        .thenReturn(0);

    assertThrows(
        AdminService.ConcurrentUpdateError.class,
        () -> adminService.finalizeAccreditation(uuid, FinalStatus.EXPIRED));
  }
}
