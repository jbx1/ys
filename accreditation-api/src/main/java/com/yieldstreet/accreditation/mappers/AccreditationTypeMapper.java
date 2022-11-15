package com.yieldstreet.accreditation.mappers;

import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.model.AccreditationType;

public class AccreditationTypeMapper {

    private AccreditationTypeMapper() {
    }

    public static Accreditation.AccreditationType mapAccreditationType(
            AccreditationType accreditationType) {
        return switch (accreditationType) {
            case INCOME -> Accreditation.AccreditationType.BY_INCOME;
            case NET_WORTH -> Accreditation.AccreditationType.BY_NET_WORTH;
        };
    }

    public static AccreditationType mapPersistedAccreditationType(Accreditation.AccreditationType accreditationType) {
        return switch (accreditationType) {
            case BY_INCOME -> AccreditationType.INCOME;
            case BY_NET_WORTH -> AccreditationType.NET_WORTH;
        };
    }
}
