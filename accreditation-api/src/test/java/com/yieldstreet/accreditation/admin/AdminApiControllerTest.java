package com.yieldstreet.accreditation.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yieldstreet.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static com.yieldstreet.accreditation.MockHelpers.getObjectMapper;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiControllerTest {

  @Mock private AdminService mockAdminService;

  private MockMvc mockMvc;

  private AutoCloseable closeable;

  private static final String CREATE_ACCREDITATION = "/user/accreditation";
  private static final String FINALISE_ACCREDITATION = "/user/accreditation/{accreditationId}";

  private final ObjectMapper objectMapper = getObjectMapper();

  @BeforeEach
  public void setup() {
    closeable = MockitoAnnotations.openMocks(this);

    AdminApiController adminApiController = new AdminApiController(mockAdminService);

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(adminApiController)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(this.objectMapper))
            .build();
  }

  @AfterEach
  void shutdown() throws Exception {
    closeable.close();
  }

  @Test
  void validRequest() throws Exception {
    CreateAccreditationRequest request = new CreateAccreditationRequest();
    request.setUserId("0123456789");
    request.setAccreditationType(AccreditationType.INCOME);
    request.setDocument(
        new Document()
            .name("2018.pdf")
            .mimeType("application/pdf")
            .content("952109582109582gkdsgsdg"));

    AccreditationResponse response = new AccreditationResponse().accreditationId(UUID.randomUUID());
    String expected = objectMapper.writeValueAsString(response);

    when(mockAdminService.createAccreditation(request)).thenReturn(response);

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(expected));
  }

  @Test
  void missingTags() throws Exception {

    String missingTag =
        """
                {
                  "accreditation_type": "BY_INCOME",
                  "document": {
                    "content": "ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==",
                    "mime_type": "application/pdf",
                    "name": "2018.pdf"
                  }
                }""";

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(missingTag))
        .andExpect(status().is4xxClientError());

    missingTag =
        """
                {
                  "user_id": "1231421",
                  "document": {
                    "content": "ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==",
                    "mime_type": "application/pdf",
                    "name": "2018.pdf"
                  }
                }""";

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(missingTag))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void missingDocumentDetails() throws Exception {
    String missingDocDetails =
        """
                {
                  "user_id": "1231421",
                  "accreditation_type": "BY_INCOME",
                  "document": {
                    "mime_type": "application/pdf",
                    "name": "2018.pdf"
                  }
                }""";

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(missingDocDetails))
        .andExpect(status().is4xxClientError());

    missingDocDetails =
        """
                {
                  "user_id": "1231421",
                  "accreditation_type": "BY_INCOME",
                  "document": {
                    "content": "ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==",
                    "name": "2018.pdf"
                  }
                }""";

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(missingDocDetails))
        .andExpect(status().is4xxClientError());

    missingDocDetails =
        """
                {
                  "user_id": "1231421",
                  "accreditation_type": "BY_INCOME",
                  "document": {
                    "content": "ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==",
                    "mime_type": "application/pdf"
                  }
                }""";

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(missingDocDetails))
        .andExpect(status().is4xxClientError());

    missingDocDetails =
        """
                {
                  "user_id": "1231421",
                  "accreditation_type": "BY_INCOME"
                }""";

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(missingDocDetails))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void emptyUserId() throws Exception {
    CreateAccreditationRequest request = new CreateAccreditationRequest();
    request.setUserId("");
    request.setAccreditationType(AccreditationType.INCOME);
    request.setDocument(
        new Document()
            .name("2018.pdf")
            .mimeType("application/pdf")
            .content("952109582109582gkdsgsdg"));

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void emptyType() throws Exception {
    CreateAccreditationRequest request = new CreateAccreditationRequest();
    request.setUserId("1345215");
    request.setDocument(
        new Document()
            .name("2018.pdf")
            .mimeType("application/pdf")
            .content("952109582109582gkdsgsdg"));

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void invalidType() throws Exception {
    String invalidType =
        """
                {
                  "user_id": "1231421",
                  "accreditation_type": "BY_NOTHING",
                  "document": {
                    "name": "2018.pdf",
                    "content": "ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==",
                    "mime_type": "application/pdf"
                  }
                }""";

    mockMvc
        .perform(
            post(CREATE_ACCREDITATION)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(invalidType))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void finalizeAccreditationConfirmed() throws Exception {

    UUID uuid = UUID.randomUUID();
    FinalStatus finalStatus = FinalStatus.CONFIRMED;

    AccreditationResponse response = new AccreditationResponse().accreditationId(uuid);

    when(mockAdminService.finalizeAccreditation(uuid, finalStatus))
        .thenReturn(Optional.of(response));

    FinaliseAccreditationRequest request = new FinaliseAccreditationRequest();
    request.setOutcome(finalStatus);

    mockMvc
        .perform(
            put(FINALISE_ACCREDITATION, uuid.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json(objectMapper.writeValueAsString(response)));
  }

  @Test
  void finalizeAccreditationFailed() throws Exception {

    UUID uuid = UUID.randomUUID();
    FinalStatus finalStatus = FinalStatus.FAILED;

    AccreditationResponse response = new AccreditationResponse().accreditationId(uuid);

    when(mockAdminService.finalizeAccreditation(uuid, finalStatus))
            .thenReturn(Optional.of(response));

    FinaliseAccreditationRequest request = new FinaliseAccreditationRequest();
    request.setOutcome(finalStatus);

    mockMvc
            .perform(
                    put(FINALISE_ACCREDITATION, uuid.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json(objectMapper.writeValueAsString(response)));
  }

  @Test
  void finalizeAccreditationExpired() throws Exception {

    UUID uuid = UUID.randomUUID();
    FinalStatus finalStatus = FinalStatus.EXPIRED;

    AccreditationResponse response = new AccreditationResponse().accreditationId(uuid);

    when(mockAdminService.finalizeAccreditation(uuid, finalStatus))
            .thenReturn(Optional.of(response));

    FinaliseAccreditationRequest request = new FinaliseAccreditationRequest();
    request.setOutcome(finalStatus);

    mockMvc
            .perform(
                    put(FINALISE_ACCREDITATION, uuid.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json(objectMapper.writeValueAsString(response)));
  }

  @Test
  void finaliseWithInvalidUuid() throws Exception {

    FinalStatus finalStatus = FinalStatus.CONFIRMED;
    FinaliseAccreditationRequest request = new FinaliseAccreditationRequest();
    request.setOutcome(finalStatus);

    mockMvc
            .perform(
                    put(FINALISE_ACCREDITATION, "1234455646346")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is4xxClientError());
  }

  @Test
  void finaliseWithInvalidStatus() throws Exception {
    UUID uuid = UUID.randomUUID();

    String request = """
            {
              "outcome": "PENDING"
            }""";

    mockMvc
            .perform(
                    put(FINALISE_ACCREDITATION, uuid.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(request))
            .andExpect(status().is4xxClientError());
  }
}
