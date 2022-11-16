package com.yieldstreet.accreditation.user;

import com.yieldstreet.accreditation.MockHelpers;
import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.accreditation.persistence.AccreditationRepository;
import com.yieldstreet.accreditation.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class UserServiceTest {
    @Mock
    UserRepository mockUserRepository;

    @Mock
    AccreditationRepository mockAccreditationRepository;

    private AutoCloseable closeable;

    private UserService userService;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);

        userService = new UserService(mockUserRepository, mockAccreditationRepository);
    }

    @AfterEach
    void shutdown() throws Exception {
        closeable.close();
    }

    @Test
    void userNotFound() {
        String userId = "214214";

        when(mockUserRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        assertThrows(UserService.UserNotFound.class,
                () -> userService.getUserAccreditations(userId));
    }

    @Test
    void userFoundButEmpty() {
        String userId = "214214";

        when(mockUserRepository.findByUserId(userId))
                .thenReturn(Optional.of(MockHelpers.mockUser(userId)));

        List<Accreditation> userAccreditations = userService.getUserAccreditations(userId);
        assertTrue(userAccreditations.isEmpty());
    }

    @Test
    void dataFoundWithoutUserCheckNeeded() {
        String userId = "214214";
        int howMany = 10;
        when(mockAccreditationRepository.findByUserUserIdOrderByCreatedTs(userId))
                .thenReturn(MockHelpers.mockAccreditations(userId, howMany));

        List<Accreditation> userAccreditations = userService.getUserAccreditations(userId);
        assertFalse(userAccreditations.isEmpty());
        assertEquals(howMany, userAccreditations.size());
    }

}
