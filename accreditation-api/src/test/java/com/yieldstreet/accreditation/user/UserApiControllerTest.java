package com.yieldstreet.accreditation.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yieldstreet.accreditation.mappers.AccreditationMapper;
import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.model.AccreditationStatusDetails;
import com.yieldstreet.model.AccreditationStatusResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.yieldstreet.accreditation.MockHelpers.getObjectMapper;
import static com.yieldstreet.accreditation.MockHelpers.mockAccreditations;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiControllerTest {
    @Mock
    private UserService mockUserService;

    private MockMvc mockMvc;

    private AutoCloseable closeable;

    private static final String USER_ACCREDITATIONS = "/user/{userId}/accreditation";

    private final ObjectMapper objectMapper = getObjectMapper();

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);

        UserApiController userApiController = new UserApiController(mockUserService);

        this.mockMvc = MockMvcBuilders.standaloneSetup(userApiController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(this.objectMapper))
                .build();
    }

    @AfterEach
    void shutdown() throws Exception {
        closeable.close();
    }

    @Test
    void getAccreditationNoData() throws Exception {
        String userId = "1234567890";

        mockMvc.perform(get(USER_ACCREDITATIONS, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getAccreditationsForEmptyUserId() throws Exception {
        String userId = "";

        mockMvc.perform(get(USER_ACCREDITATIONS, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAccreditationsForUnknownUser() throws Exception {
        String userId = "1234567890";

        when(mockUserService.getUserAccreditations(userId))
                .thenThrow(new UserService.UserNotFound());

        mockMvc.perform(get(USER_ACCREDITATIONS, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAccreditationForUserWithData() throws Exception {
        String userId = "1234567890";

        List<Accreditation> accreditations = mockAccreditations(userId, 10);

        Map<String, AccreditationStatusDetails> statusDetailsMap = accreditations.stream()
                .collect(Collectors.toMap(accreditation -> accreditation.getId().toString(), AccreditationMapper::mapToStatusDetails));

        AccreditationStatusResponse expectedResponse = new AccreditationStatusResponse();
        expectedResponse.setAccreditationStatuses(statusDetailsMap);
        expectedResponse.setUserId(userId);

        String expected = objectMapper.writeValueAsString(expectedResponse);

        when(mockUserService.getUserAccreditations(userId))
                .thenReturn(accreditations);

        mockMvc.perform(get(USER_ACCREDITATIONS, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expected));
    }
}
