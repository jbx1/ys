package com.yieldstreet.accreditation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.accreditation.persistence.User;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MockHelpers {
  private static final Random RNG = new Random();

  private MockHelpers() {}

  public static ObjectMapper getObjectMapper() {
    return JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .build()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public static User mockUser(String userId) {
    User user = new User();
    user.setId(RNG.nextInt());
    user.setUserId(userId);
    user.setCreatedTs(OffsetDateTime.now());
    return user;
  }

  public static List<Accreditation> mockAccreditations(String userId, int count) {
    return Stream.generate(
            () ->
                mockAccreditation(
                    UUID.randomUUID(), mockUser(userId), Accreditation.AccreditationStatus.PENDING))
        .limit(count)
        .collect(Collectors.toList());
  }

  public static Accreditation mockAccreditation(
      UUID uuid, User user, Accreditation.AccreditationStatus status) {
    Accreditation accreditation = new Accreditation();
    accreditation.setId(uuid);
    accreditation.setUser(user);
    accreditation.setCreatedTs(OffsetDateTime.now());
    accreditation.setUpdatedTs(OffsetDateTime.now());
    accreditation.setDocumentName(RNG.nextInt(1994, 2022) + ".pdf");
    accreditation.setDocumentMimeType("application/pdf");
    accreditation.setDocumentContent(randomString(RNG.nextInt(20, 100)));
    accreditation.setType(randomEnum(Accreditation.AccreditationType.class));
    accreditation.setStatus(status);

    return accreditation;
  }

  public static String randomString(int length) {
    // the ascii range of printable characters including space
    String raw =
        RNG.ints(' ', '~' + 1)
            .limit(length)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();

    // encode it to base64
    return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  public static <T extends Enum<T>> T randomEnum(Class<T> enumClass) {
    T[] enumConstants = enumClass.getEnumConstants();
    return enumConstants[RNG.nextInt(enumConstants.length)];
  }
}
