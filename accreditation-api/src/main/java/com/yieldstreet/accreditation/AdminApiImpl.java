package com.yieldstreet.accreditation;

import com.yieldstreet.api.AdminApi;
import com.yieldstreet.model.CreateAccreditationRequest;
import com.yieldstreet.model.CreateAccreditationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminApiImpl implements AdminApi {

    @Override
    public ResponseEntity<CreateAccreditationResponse> createAccreditation(CreateAccreditationRequest createAccreditationRequest) {
        return AdminApi.super.createAccreditation(createAccreditationRequest);
    }
}
