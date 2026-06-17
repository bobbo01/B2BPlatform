package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.dto.FirstCompanyAdminRequestView;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.entity.FirstCompanyAdminRequest;
import com.bobbo01.supplyhub.domain.company.repository.FirstCompanyAdminRequestRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FirstCompanyAdminRequestService {

    private final FirstCompanyAdminRequestRepository firstCompanyAdminRequestRepository;
    private final UserRepository userRepository;

    public FirstCompanyAdminRequestService(
            FirstCompanyAdminRequestRepository firstCompanyAdminRequestRepository,
            UserRepository userRepository
    ) {
        this.firstCompanyAdminRequestRepository = firstCompanyAdminRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createRequest(Long requesterUserId) {
        User requester = getRequiredUser(requesterUserId);
        assertEligibleRequester(requester);
        assertNoPendingRequest(requester);

        firstCompanyAdminRequestRepository.save(FirstCompanyAdminRequest.createPending(requester.getCompany(), requester));
    }

    @Transactional
    public void cancelOwnPendingRequest(Long requesterUserId) {
        User requester = getRequiredUser(requesterUserId);
        FirstCompanyAdminRequest request = firstCompanyAdminRequestRepository
                .findByRequesterIdAndStatus(requesterUserId, FirstCompanyAdminRequest.STATUS_PENDING)
                .orElseThrow(() -> new IllegalStateException("취소할 승인 대기 요청이 없습니다."));

        if (!request.getRequester().getId().equals(requester.getId())) {
            throw new IllegalStateException("자기 요청만 취소할 수 있습니다.");
        }

        request.cancel();
    }

    @Transactional
    public void approveRequest(Long reviewerUserId, Long requestId) {
        User reviewer = getRequiredUser(reviewerUserId);
        assertPlatformAdmin(reviewer);

        FirstCompanyAdminRequest request = getPendingRequest(requestId);
        User requester = request.getRequester();
        requester.grantCompanyAdmin();
        request.approve(reviewer, null);
    }

    @Transactional
    public void rejectRequest(Long reviewerUserId, Long requestId) {
        User reviewer = getRequiredUser(reviewerUserId);
        assertPlatformAdmin(reviewer);

        FirstCompanyAdminRequest request = getPendingRequest(requestId);
        request.reject(reviewer, null);
    }

    public Optional<FirstCompanyAdminRequestView> findLatestRequestForUser(Long userId) {
        return firstCompanyAdminRequestRepository.findFirstByRequesterIdOrderByCreatedAtDesc(userId)
                .map(this::toView);
    }

    public List<FirstCompanyAdminRequestView> getPendingRequestsForPlatformAdmin(Long reviewerUserId) {
        User reviewer = getRequiredUser(reviewerUserId);
        assertPlatformAdmin(reviewer);

        return firstCompanyAdminRequestRepository.findAllByStatusOrderByCreatedAtAsc(FirstCompanyAdminRequest.STATUS_PENDING)
                .stream()
                .map(this::toView)
                .toList();
    }

    public boolean canCreateRequest(Long requesterUserId) {
        User requester = getRequiredUser(requesterUserId);
        if (requester.isPlatformAdmin() || requester.getCompany() == null || requester.hasCompanyAdminRole()) {
            return false;
        }
        Company company = requester.getCompany();
        if (!requester.getId().equals(company.getCreatorUserId())) {
            return false;
        }
        return firstCompanyAdminRequestRepository.findByRequesterIdAndStatus(requesterUserId, FirstCompanyAdminRequest.STATUS_PENDING).isEmpty()
                && firstCompanyAdminRequestRepository.findByCompanyIdAndStatus(company.getId(), FirstCompanyAdminRequest.STATUS_PENDING).isEmpty();
    }

    private void assertEligibleRequester(User requester) {
        if (requester.isPlatformAdmin()) {
            throw new IllegalStateException("PLATFORM_ADMIN 계정은 첫 COMPANY_ADMIN 요청 대상이 아닙니다.");
        }
        if (requester.getCompany() == null) {
            throw new IllegalStateException("회사 소속 사용자만 요청할 수 있습니다.");
        }
        if (requester.hasCompanyAdminRole()) {
            throw new IllegalStateException("이미 COMPANY_ADMIN 권한을 가진 사용자입니다.");
        }
        if (!requester.getId().equals(requester.getCompany().getCreatorUserId())) {
            throw new IllegalStateException("회사 생성 완료를 최종 성공시킨 사용자만 첫 COMPANY_ADMIN 요청을 할 수 있습니다.");
        }
    }

    private void assertNoPendingRequest(User requester) {
        if (firstCompanyAdminRequestRepository.findByRequesterIdAndStatus(
                requester.getId(),
                FirstCompanyAdminRequest.STATUS_PENDING
        ).isPresent()) {
            throw new IllegalStateException("이미 승인 대기 중인 첫 COMPANY_ADMIN 요청이 있습니다.");
        }
        if (firstCompanyAdminRequestRepository.findByCompanyIdAndStatus(
                requester.getCompany().getId(),
                FirstCompanyAdminRequest.STATUS_PENDING
        ).isPresent()) {
            throw new IllegalStateException("이 회사에는 이미 승인 대기 중인 첫 COMPANY_ADMIN 요청이 있습니다.");
        }
    }

    private void assertPlatformAdmin(User reviewer) {
        if (!reviewer.isPlatformAdmin()) {
            throw new IllegalStateException("PLATFORM_ADMIN만 처리할 수 있습니다.");
        }
    }

    private FirstCompanyAdminRequest getPendingRequest(Long requestId) {
        FirstCompanyAdminRequest request = firstCompanyAdminRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("요청을 찾을 수 없습니다."));
        if (!request.isPending()) {
            throw new IllegalStateException("승인 대기 중인 요청만 처리할 수 있습니다.");
        }
        return request;
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }

    private FirstCompanyAdminRequestView toView(FirstCompanyAdminRequest request) {
        return new FirstCompanyAdminRequestView(
                request.getId(),
                request.getCompany().getId(),
                request.getCompany().getCompanyName(),
                request.getCompany().getCompanyDomain(),
                request.getRequester().getId(),
                request.getRequester().getFullName(),
                request.getRequester().getEmail(),
                request.getStatus()
        );
    }
}
