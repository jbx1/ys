package com.yieldstreet.accreditation.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yieldstreet.model.CreateAccreditationRequest;
import com.yieldstreet.model.Status;

public record AccreditationStateChange(
        @JsonProperty(value = "action", required = true) Action action,
        @JsonProperty(value = "user_id", required = true) String userId,
        @JsonProperty(value = "accreditation_id", required = true) String accreditationId,
        @JsonProperty(value = "status", required = true) Status status,
        @JsonProperty(value = "old_status", required = false) Status old_status,
        @JsonProperty(value = "request", required = true) CreateAccreditationRequest request
        ) {

    public enum Action {
        CREATE,
        FINALISE,
        SCHEDULED_EXPIRE
    }
}
