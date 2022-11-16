package com.yieldstreet.accreditation.mappers;

import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.accreditation.persistence.User;
import com.yieldstreet.model.AccreditationStatusDetails;
import org.junit.jupiter.api.Test;

import static com.yieldstreet.accreditation.MockHelpers.mockAccreditation;
import static com.yieldstreet.accreditation.MockHelpers.mockUser;
import static org.junit.jupiter.api.Assertions.*;

class AccreditationMapperTest {

    @Test
    void mapToStatusDetails() {
        String userId = "0124152";
        User user = mockUser(userId);
        Accreditation accreditation = mockAccreditation(user);

        AccreditationStatusDetails accreditationStatusDetails = AccreditationMapper.mapToStatusDetails(accreditation);
        assertEquals(StatusMapper.mapToStatus(accreditation.getStatus()), accreditationStatusDetails.getStatus());
        assertEquals(AccreditationTypeMapper.mapPersistedAccreditationType(accreditation.getType()), accreditationStatusDetails.getAccreditationType());
    }

}