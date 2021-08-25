package org.upgrad.upstac.testrequests.consultation;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.config.security.UserLoggedInService;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.testrequests.RequestStatus;
import org.upgrad.upstac.testrequests.TestRequest;
import org.upgrad.upstac.testrequests.TestRequestQueryService;
import org.upgrad.upstac.testrequests.TestRequestUpdateService;
import org.upgrad.upstac.testrequests.flow.TestRequestFlowService;
import org.upgrad.upstac.users.User;

import javax.validation.ConstraintViolationException;
import java.util.List;

import static org.upgrad.upstac.exception.UpgradResponseStatusException.asBadRequest;
import static org.upgrad.upstac.exception.UpgradResponseStatusException.asConstraintViolation;


@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    Logger log = LoggerFactory.getLogger(ConsultationController.class);

    @Autowired
    private TestRequestUpdateService testRequestUpdateService;

    @Autowired
    private TestRequestQueryService testRequestQueryService;

    @Autowired
    TestRequestFlowService  testRequestFlowService;

    @Autowired
    private UserLoggedInService userLoggedInService;

    @GetMapping("/in-queue")
    @PreAuthorize("hasAnyRole('DOCTOR')")
    public List<TestRequest> getForConsultations()  {
        // Fetch the tests that have been completed by the tester and need medical consultation (by doctor)
        return testRequestQueryService.findBy(RequestStatus.LAB_TEST_COMPLETED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR')")
    public List<TestRequest> getForDoctor()  {
        // Fetch the current user that is logged in
        User currentLoggedInUser = userLoggedInService.getLoggedInUser();
        return testRequestQueryService.findByDoctor(currentLoggedInUser);
    }

    @PreAuthorize("hasAnyRole('DOCTOR')")
    @PutMapping("/assign/{id}")
    public TestRequest assignForConsultation(@PathVariable Long id) {
        // Fetch the current user that is logged in
        User currentLoggedInDoctor = userLoggedInService.getLoggedInUser();
        try {
            return testRequestUpdateService.assignForConsultation(id, currentLoggedInDoctor);
        }catch (AppException e) {
            // Throw BADRequest in case of AppException
            throw asBadRequest(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('DOCTOR')")
    @PutMapping("/update/{id}")
    public TestRequest updateConsultation(@PathVariable Long id,@RequestBody CreateConsultationRequest testResult) {
        // Fetch the current user that is logged in
        User currentLoggedInUser = userLoggedInService.getLoggedInUser();
        try {
            // Return only those TestRequest that the doctor is allowed to update consultation for
            return testRequestUpdateService.updateConsultation(id, testResult, currentLoggedInUser);
        } catch (ConstraintViolationException e) {
            throw asConstraintViolation(e);
        }catch (AppException e) {
            // Throw BADRequest in case of AppException
            throw asBadRequest(e.getMessage());
        }
    }
}
