package com.grash.utils;

import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.UserInvitationDTO;
import com.grash.dto.UserSignupRequest;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PermissionType;
import com.grash.security.CustomUserDetail;
import com.grash.security.JwtTokenProvider;
import com.grash.service.RoleService;
import com.grash.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class UserTestUtils {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RoleService roleService;

    public OwnUser generateUserAndEnable() {
        OwnUser user = generateDisabledUser();
        user.setEnabled(true);
        return userService.save(user);
    }

    public OwnUser generateWithoutPrivilege(OwnUser inviter, PermissionType permissionType,
                                            PermissionEntity permissionEntity) {
        Role roleWithoutPrivilege = roleService.findByCompany(inviter.getCompany().getId()).stream()
                .filter(role -> !hasPermission(role, permissionType, permissionEntity))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No role found without the specified privilege"));

        OwnUser user = generateUserAndEnable();
        user.setCompany(inviter.getCompany());
        user.setRole(roleWithoutPrivilege);
        return userService.save(user);
    }

    private boolean hasPermission(Role role, PermissionType type, PermissionEntity entity) {
        switch (type) {
            case CREATE:
                return role.getCreatePermissions().contains(entity);
            case VIEW:
                return role.getViewPermissions().contains(entity);
            case VIEW_OTHER:
                return role.getViewOtherPermissions().contains(entity);
            case EDIT_OTHER:
                return role.getEditOtherPermissions().contains(entity);
            case DELETE_OTHER:
                return role.getDeleteOtherPermissions().contains(entity);
            default:
                return false;
        }
    }


    public OwnUser generateWithPrivilege(OwnUser inviter, PermissionType permissionType,
                                         PermissionEntity permissionEntity) {
        Role roleWithPrivilege = roleService.findByCompany(inviter.getCompany().getId()).stream()
                .filter(role -> hasPermission(role, permissionType, permissionEntity))
                .min(Comparator.comparingInt(this::getTotalPermissionsCount))
                .orElseThrow(() -> new IllegalStateException("No role found with the specified privilege"));

        return generateWithRole(inviter, roleWithPrivilege);
    }

    private int getTotalPermissionsCount(Role role) {
        return role.getCreatePermissions().size()
                + role.getViewPermissions().size()
                + role.getViewOtherPermissions().size()
                + role.getEditOtherPermissions().size()
                + role.getDeleteOtherPermissions().size();
    }

    public OwnUser generateWithPrivileges(OwnUser inviter,
                                          List<Pair<PermissionType, PermissionEntity>> requiredPermissions) {
        Collection<Role> roles = roleService.findByCompany(inviter.getCompany().getId());

        Optional<Role> roleWithAllPermissions = roles.stream()
                .filter(role -> requiredPermissions.stream()
                        .allMatch(pair -> hasPermission(role, pair.getFirst(), pair.getSecond())))
                .min(Comparator.comparingInt(this::getTotalPermissionsCount));

        return roleWithAllPermissions
                .map(role -> generateWithRole(inviter, role))
                .orElseThrow(() -> new IllegalStateException("No role found with the required permissions"));
    }

    public OwnUser generateWithRole(OwnUser inviter, Role role) {
        String invitedUserEmail = TestHelper.generateEmail();
        userService.invite(inviter, UserInvitationDTO.builder()
                .emails(Arrays.asList(invitedUserEmail))
                .role(role).build());

        UserSignupRequest userSignupRequest = UserSignupRequest.builder()
                .role(role)
                .firstName(TestHelper.generateString())
                .lastName(TestHelper.generateString())
                .email(invitedUserEmail)
                .password(TestHelper.generateString())
                .phone(TestHelper.generatePhone()).build();
        SignupSuccessResponse<OwnUser> signupResponse = userService.signup(userSignupRequest);
        return signupResponse.getUser();
    }

    public UserSignupRequest getRandomSignupRequest() {
        return UserSignupRequest.builder()
                .email(TestHelper.generateEmail().toLowerCase())
                .password(TestHelper.generateEightCharString())
                .firstName(TestHelper.generateEightCharString())
                .lastName(TestHelper.generateEightCharString())
                .phone(TestHelper.generatePhone())
                .companyName(TestHelper.generateEightCharString())
                .build();
    }

    public OwnUser generateDisabledUser() {
        UserSignupRequest signupRequest = getRandomSignupRequest();
        userService.signup(signupRequest);
        return userService.findByEmail(signupRequest.getEmail()).get();
    }


    public String getToken(OwnUser user) {
        return jwtTokenProvider.createToken(user.getEmail(),
                Stream.of(user.getRole()).map(Role::getRoleType).collect(Collectors.toList()));
    }

    public void setCurrentUser(OwnUser user) {
        CustomUserDetail customUserDetail =
                CustomUserDetail.builder().user(user).build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetail,
                null,
                customUserDetail.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }


    public Role getRandomRole(OwnUser clinician, Boolean paid) {
        return TestHelper.getRandomFromCollection(roleService.findByCompany(clinician.getCompany().getId()).stream().filter(role -> {
            if (paid != null) {
                return role.isPaid() == paid;
            }
            return true;
        }).collect(Collectors.toList())).get();
    }

    public Role getRandomRole(OwnUser clinician) {
        return getRandomRole(clinician, null);
    }

    public HttpHeaders getHeaders(OwnUser user) {
        String jwtToken = getToken(user);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        return headers;
    }
}
