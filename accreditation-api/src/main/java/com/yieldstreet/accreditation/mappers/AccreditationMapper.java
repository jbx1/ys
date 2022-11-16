package com.yieldstreet.accreditation.mappers;

import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.model.AccreditationStatusDetails;
import com.yieldstreet.model.AccreditationType;
import com.yieldstreet.model.Status;

public class AccreditationMapper {

    private AccreditationMapper() {
    }

    public static AccreditationStatusDetails mapToStatusDetails(Accreditation accreditation) {
        Status status = StatusMapper.mapToStatus(accreditation.getStatus());
        AccreditationType accreditationType =
                AccreditationTypeMapper.mapPersistedAccreditationType(accreditation.getType());

        AccreditationStatusDetails accreditationStatusDetails = new AccreditationStatusDetails();
        accreditationStatusDetails.setStatus(status);
        accreditationStatusDetails.setAccreditationType(accreditationType);

        return accreditationStatusDetails;
    }
}
