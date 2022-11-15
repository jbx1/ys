package com.yieldstreet.accreditation.mappers;

import com.yieldstreet.accreditation.persistence.Accreditation;
import com.yieldstreet.model.FinalStatus;
import com.yieldstreet.model.Status;

/**
 * Some utility methods to map between the model Status objects and the database.
 */
public class StatusMapper {

    private StatusMapper() {
    }

    public static Accreditation.AccreditationStatus mapToDatabaseStatus(FinalStatus finalStatus) {
        return switch (finalStatus) {
            case FAILED -> Accreditation.AccreditationStatus.FAILED;
            case CONFIRMED -> Accreditation.AccreditationStatus.CONFIRMED;
            case EXPIRED -> Accreditation.AccreditationStatus.EXPIRED;
        };
    }

    public static Status mapToStatus(FinalStatus status) {
        return switch (status) {
            case FAILED -> Status.FAILED;
            case EXPIRED -> Status.EXPIRED;
            case CONFIRMED -> Status.CONFIRMED;
        };
    }

    public static Status mapToStatus(Accreditation.AccreditationStatus status) {
        return switch (status) {
            case FAILED -> Status.FAILED;
            case EXPIRED -> Status.EXPIRED;
            case PENDING -> Status.PENDING;
            case CONFIRMED -> Status.CONFIRMED;
        };
    }
}
