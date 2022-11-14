package com.yieldstreet.accreditation.user;

import com.yieldstreet.api.UserApi;
import com.yieldstreet.model.AccreditationStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserApiImpl implements UserApi {
    @Override
    public ResponseEntity<AccreditationStatusResponse> getAccreditationStatuses(String userId) {
        return UserApi.super.getAccreditationStatuses(userId);
    }
}
