package com.yieldstreet.accreditation.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yieldstreet.model.AccreditationResponse;
import com.yieldstreet.model.AccreditationType;
import com.yieldstreet.model.CreateAccreditationRequest;
import com.yieldstreet.model.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static com.yieldstreet.accreditation.MockHelpers.getObjectMapper;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiControllerTest {

    @Mock
    private AdminService mockAdminService;

    private MockMvc mockMvc;

    private AutoCloseable closeable;

    private static final String USER_ACCREDITATION = "/user/accreditation";

    private final ObjectMapper objectMapper = getObjectMapper();

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);

        AdminApiController adminApiController = new AdminApiController(mockAdminService);

        this.mockMvc = MockMvcBuilders.standaloneSetup(adminApiController)
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
        request.setDocument(new Document()
                .name("2018.pdf")
                .mimeType("application/pdf")
                .content("952109582109582gkdsgsdg"));

        AccreditationResponse response = new AccreditationResponse().accreditationId(UUID.randomUUID());
        String expected = objectMapper.writeValueAsString(response);

        when(mockAdminService.createAccreditation(request))
                .thenReturn(response);

        mockMvc.perform(post(USER_ACCREDITATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expected));
    }

    @Test
    void missingTags() throws Exception {

        String missingTag = "{\n" +
                "  \"accreditation_type\": \"BY_INCOME\",\n" +
                "  \"document\": {\n" +
                "    \"content\": \"ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==\",\n" +
                "    \"mime_type\": \"application/pdf\",\n" +
                "    \"name\": \"2018.pdf\"\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post(USER_ACCREDITATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(missingTag))
                .andExpect(status().is4xxClientError());

        missingTag = "{\n" +
                " \"user_id\": \"1231421\",\n" +
                "  \"document\": {\n" +
                "    \"content\": \"ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==\",\n" +
                "    \"mime_type\": \"application/pdf\",\n" +
                "    \"name\": \"2018.pdf\"\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post(USER_ACCREDITATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(missingTag))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void missingDocumentDetails() throws Exception {
        String missingDocDetails = "{\n" +
                " \"user_id\": \"1231421\",\n" +
                "  \"accreditation_type\": \"BY_INCOME\",\n" +
                "  \"document\": {\n" +
                "    \"mime_type\": \"application/pdf\",\n" +
                "    \"name\": \"2018.pdf\"\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post(USER_ACCREDITATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(missingDocDetails))
                .andExpect(status().is4xxClientError());

        missingDocDetails = "{\n" +
                " \"user_id\": \"1231421\",\n" +
                "  \"accreditation_type\": \"BY_INCOME\",\n" +
                "  \"document\": {\n" +
                "    \"content\": \"ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==\",\n" +
                "    \"name\": \"2018.pdf\"\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post(USER_ACCREDITATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(missingDocDetails))
                .andExpect(status().is4xxClientError());

        missingDocDetails = "{\n" +
                " \"user_id\": \"1231421\",\n" +
                "  \"accreditation_type\": \"BY_INCOME\",\n" +
                "  \"document\": {\n" +
                "    \"content\": \"ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==\",\n" +
                "    \"mime_type\": \"application/pdf\"\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post(USER_ACCREDITATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(missingDocDetails))
                .andExpect(status().is4xxClientError());

        missingDocDetails = "{\n" +
                " \"user_id\": \"1231421\",\n" +
                "  \"accreditation_type\": \"BY_INCOME\"\n" +
                "}";

        mockMvc.perform(post(USER_ACCREDITATION)
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
        request.setDocument(new Document()
                .name("2018.pdf")
                .mimeType("application/pdf")
                .content("952109582109582gkdsgsdg"));

        mockMvc.perform(post(USER_ACCREDITATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void emptyType() throws Exception {
        CreateAccreditationRequest request = new CreateAccreditationRequest();
        request.setUserId("1345215");
        request.setDocument(new Document()
                .name("2018.pdf")
                .mimeType("application/pdf")
                .content("952109582109582gkdsgsdg"));

        mockMvc.perform(post(USER_ACCREDITATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void invalidType() throws Exception {
        String invalidType = "{\n" +
                " \"user_id\": \"1231421\",\n" +
                "  \"accreditation_type\": \"BY_NOTHING\",\n" +
                "  \"document\": {\n" +
                "    \"name\": \"2018.pdf\",\n" +
                "    \"content\": \"ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==\",\n" +
                "    \"mime_type\": \"application/pdf\"\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post(USER_ACCREDITATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(invalidType))
                .andExpect(status().is4xxClientError());
    }
}
