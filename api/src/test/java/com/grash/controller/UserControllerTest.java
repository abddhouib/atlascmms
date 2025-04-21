//package com.grash.controller;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.grash.CustomPostgresSQLContainer;
//import com.grash.dto.*;
//import com.grash.exception.CustomException;
//import com.grash.model.OwnUser;
//import com.grash.model.Role;
//import com.grash.model.Subscription;
//import com.grash.model.enums.Language;
//import com.grash.service.SubscriptionService;
//import com.grash.service.UserService;
//import com.grash.utils.Helper;
//import com.grash.utils.TestHelper;
//import com.grash.utils.UserTestUtils;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.SpyBean;
//import org.springframework.context.MessageSource;
//import org.springframework.data.domain.Page;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Stream;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class UserControllerTest extends CustomPostgresSQLContainer {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private UserTestUtils userTestUtils;
//
//    @SpyBean
//    private UserService userService;
//
//    @Autowired
//    private MessageSource messageSource;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private SubscriptionService subscriptionService;
//
//    @Test
//    public void searchShouldReturnUsersWhenUserHasViewSettingsPrivilege() throws Exception {
//        OwnUser inviter = userTestUtils.generateUserAndEnable();
//        Role role = inviter.getDoctorRole();
//        UserSignupRequest validSignupRequest = userTestUtils.getRandomSignupRequest(inviter.getCompany().getType());
//        userService.invite(validSignupRequest.getEmail(), role, inviter);
//        validSignupRequest.setRole(role);
//
//        SignupSuccessResponse<OwnUser> signupSuccessResponse = userService.signup(validSignupRequest, Language.EN);
//        String jwtToken = userTestUtils.getToken(inviter);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(jwtToken);
//
//        MvcResult result = mockMvc.perform(post("/users/search")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{}")
//                        .headers(headers))
//                .andExpect(status().isOk()).andReturn();
//
//        String responseContent = result.getResponse().getContentAsString();
//        Page<UserResponseDTO> usersResponse = objectMapper.readValue(
//                responseContent,
//                new TypeReference<RestResponsePage<UserResponseDTO>>() {
//                }
//        );
//        assertEquals(2, usersResponse.getTotalElements());
//        for (UserResponseDTO user : usersResponse) {
//            assertTrue(Stream.of(inviter, signupSuccessResponse.getUser()).anyMatch(u -> u.getId().equals(user
//            .getId())));
//        }
//    }
//
//    @Test
//    public void searchShouldReturnForbiddenWhenUserDoesNotHaveViewSettingsPrivilege() throws Exception {
//        OwnUser inviter = userTestUtils.generateUserAndEnable();
//        OwnUser clinician = userTestUtils.generateWithoutPrivilege(inviter, PrivilegeEnum.VIEW_SETTINGS);
//        String jwtToken = userTestUtils.getToken(clinician);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(jwtToken);
//        String expectedMessage = messageSource.getMessage("access_denied", null, clinician.getLocale());
//
//        mockMvc.perform(post("/users/search")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{}")
//                        .headers(headers))
//                .andExpect(status().isForbidden())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException) {
//                        CustomException ex = (CustomException) result.getResolvedException();
//                        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
//                    } else {
//                        fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void searchShouldReturnForbiddenWhenUserIsNotAuthenticated() throws Exception {
//        mockMvc.perform(post("/users/search")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{}"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void inviteShouldSucceedWhenUserHasPrivilegeAndSubscriptionAllows() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//        String jwtToken = userTestUtils.getToken(user);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(jwtToken);
//
//        Role role = userTestUtils.getRandomRole(user, true);
//        UserInvitationDTO invitation = new UserInvitationDTO();
//        List<String> emails = List.of(TestHelper.generateEmail(), TestHelper.generateEmail());
//        invitation.setEmails(emails);
//        invitation.setRole(role);
//
//        Mockito.reset(emailService);
//        mockMvc.perform(post("/users/invite")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(invitation))
//                        .headers(headers))
//                .andExpect(status().isOk());
//
//        verify(userService, times(emails.size())).invite(anyString(), any(), any());
//        verify(emailService, times(emails.size())).sendMessageUsingThymeleafTemplate(any(), any(), any(), any(),
//                any(), any());
//    }
//
//
//    @Test
//    public void inviteShouldThrowExceptionWhenSubscriptionLimitIsReached() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//        String jwtToken = userTestUtils.getToken(user);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(jwtToken);
//        Subscription subscription = user.getCompany().getSubscription();
//        subscription.setUsersCount(2);
//        subscriptionService.save(subscription);
//
//        UserInvitationDTO invitation = new UserInvitationDTO();
//        List<String> emails = List.of(TestHelper.generateEmail(), TestHelper.generateEmail());
//        invitation.setEmails(emails);
//        Role role = userTestUtils.getRandomRole(user, true);
//        invitation.setRole(role);
//
//        mockMvc.perform(post("/users/invite")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(invitation))
//                        .headers(headers))
//                .andExpect(status().isNotAcceptable())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException) {
//                        CustomException ex = (CustomException) result.getResolvedException();
//                        assertThat(ex.getMessage()).isEqualTo("Your current subscription doesn't allow you" +
//                                " to invite that many users");
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
//                    } else {
//                        fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void inviteShouldThrowExceptionWhenRoleNotFound() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//        String jwtToken = userTestUtils.getToken(user);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(jwtToken);
//
//        UserInvitationDTO invitation = new UserInvitationDTO();
//        List<String> emails = List.of(TestHelper.generateEmail());
//        invitation.setEmails(emails);
//        Role role = new Role();
//        role.setId((long) TestHelper.generateRandomInt(4));
//        invitation.setRole(role);
//
//        mockMvc.perform(post("/users/invite")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(invitation))
//                        .headers(headers))
//                .andExpect(status().isNotFound());
//    }
//
//    private HttpHeaders getAuthHeaders(OwnUser user) {
//        String token = userTestUtils.getToken(user);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(token);
//        return headers;
//    }
//
//    @Test
//    public void getMiniShouldReturnAllEnabledUsersWhenNoFiltersProvided() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//        int newUsersCount = 2;
//        List<OwnUser> users = new ArrayList<>();
//        users.add(user);
//        for (int i = 0; i < newUsersCount; i++) {
//            users.add(userTestUtils.generateWithRole(user,
//                    userTestUtils.getRandomRole(user)));
//        }
//
//        mockMvc.perform(get("/users/mini")
//                        .headers(getAuthHeaders(user))
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(result -> {
//                    String content = result.getResponse().getContentAsString();
//                    List<ClinicianMiniDTO> cliniciansResponse = objectMapper.readValue(content,
//                            new TypeReference<>() {
//                            });
//                    assertEquals(users.size(), cliniciansResponse.size());
//                    List<Long> expectedIds = users.stream().map(OwnUser::getId).toList();
//                    List<Long> actualIds =
//                            cliniciansResponse.stream().map(ClinicianMiniDTO::getId).toList();
//                    assertThat(expectedIds).containsExactlyInAnyOrderElementsOf(actualIds);
//                });
//    }
//
//    @Test
//    public void getMiniShouldFilterByPrivilege() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//        PrivilegeEnum privilegeEnum = PrivilegeEnum.VIEW_SETTINGS;
//        OwnUser userWithoutPrivilege = userTestUtils.generateWithoutPrivilege(user, privilegeEnum);
//
//        mockMvc.perform(get("/users/mini")
//                        .headers(getAuthHeaders(user))
//                        .param("privilegeEnum", privilegeEnum.name())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(result -> {
//                    String content = result.getResponse().getContentAsString();
//                    List<ClinicianMiniDTO> cliniciansResponse = objectMapper.readValue(content,
//                            new TypeReference<>() {
//                            });
//                    assertThat(cliniciansResponse.size()).isEqualTo(1);
//                    assertThat(cliniciansResponse.get(0).getId()).isEqualTo(user.getId());
//                });
//    }
//
//    @Test
//    public void getMiniShouldFilterByEnabledStatus() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//
//        OwnUser disabledUser = userTestUtils.generateWithRole(user, userTestUtils.getRandomRole(user));
//        disabledUser.setEnabled(false);
//        userService.save(disabledUser);
//
//        mockMvc.perform(get("/users/mini")
//                        .headers(getAuthHeaders(user))
//                        .param("enabled", "true")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(result -> {
//                    String content = result.getResponse().getContentAsString();
//                    List<ClinicianMiniDTO> cliniciansResponse = objectMapper.readValue(content,
//                            new TypeReference<>() {
//                            });
//                    assertThat(cliniciansResponse.size()).isEqualTo(1);
//                    assertThat(cliniciansResponse.get(0).getId()).isEqualTo(user.getId());
//                });
//
//        mockMvc.perform(get("/users/mini")
//                        .headers(getAuthHeaders(user))
//                        .param("enabled", "false")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(result -> {
//                    String content = result.getResponse().getContentAsString();
//                    List<ClinicianMiniDTO> cliniciansResponse = objectMapper.readValue(content,
//                            new TypeReference<>() {
//                            });
//                    assertThat(cliniciansResponse.size()).isEqualTo(1);
//                    assertThat(cliniciansResponse.get(0).getId()).isEqualTo(disabledUser.getId());
//                });
//    }
//
//    @Test
//    public void getMiniShouldReturnForbiddenForUnauthenticatedUser() throws Exception {
//        mockMvc.perform(get("/users/mini")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void patchShouldUpdateUserWhenRequestingOwnProfile() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//
//        UserPatchDTO patchDTO = new UserPatchDTO();
//        patchDTO.setFirstName(TestHelper.generateString());
//        patchDTO.setLastName(TestHelper.generateString());
//        patchDTO.setLanguage(Language.EN);
//
//        mockMvc.perform(patch("/users/{id}", user.getId())
//                        .headers(getAuthHeaders(user))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO)))
//                .andExpect(status().isOk())
//                .andExpect(result -> {
//                    String content = result.getResponse().getContentAsString();
//                    UserResponseDTO responseDTO = objectMapper.readValue(content, UserResponseDTO.class);
//                    assertThat(responseDTO.getFirstName().toLowerCase()).isEqualTo(patchDTO.getFirstName()
//                    .toLowerCase());
//                });
//    }
//
//    @Test
//    public void patchShouldUpdateUserWhenRequestingWithViewSettingsPrivilege() throws Exception {
//        OwnUser requester = userTestUtils.generateUserAndEnable();
//
//        OwnUser targetUser = userTestUtils.generateWithRole(requester, userTestUtils.getRandomRole(requester));
//
//        UserPatchDTO patchDTO = new UserPatchDTO();
//        patchDTO.setFirstName(TestHelper.generateString());
//        patchDTO.setLastName(TestHelper.generateString());
//        patchDTO.setLanguage(Language.FR);
//
//        mockMvc.perform(patch("/users/{id}", targetUser.getId())
//                        .headers(getAuthHeaders(requester))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO)))
//                .andExpect(status().isOk())
//                .andExpect(result -> {
//                    String content = result.getResponse().getContentAsString();
//                    UserResponseDTO responseDTO = objectMapper.readValue(content, UserResponseDTO.class);
//                    assertThat(responseDTO.getLastName().toLowerCase()).isEqualTo(patchDTO.getLastName()
//                    .toLowerCase());
//                });
//    }
//
//    @Test
//    public void patchShouldReturnForbiddenWhenRequestingAnotherUseInAnotherCompany() throws Exception {
//        OwnUser requester = userTestUtils.generateUserAndEnable();
//        OwnUser targetUser = userTestUtils.generateUserAndEnable();
//
//        UserPatchDTO patchDTO = new UserPatchDTO();
//        patchDTO.setFirstName(TestHelper.generateString());
//        patchDTO.setLastName(TestHelper.generateString());
//        patchDTO.setLanguage(Language.FR);
//        String expectedMessage = messageSource.getMessage("access_denied", null, requester.getLocale());
//
//        mockMvc.perform(patch("/users/{id}", targetUser.getId())
//                        .headers(getAuthHeaders(requester))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO)))
//                .andExpect(status().isForbidden())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException) {
//                        CustomException ex = (CustomException) result.getResolvedException();
//                        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
//                    } else {
//                        Assertions.fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void patchShouldReturnForbiddenWhenRequestingAnotherUseWithoutPrivilege() throws Exception {
//        OwnUser companyCreator = userTestUtils.generateUserAndEnable();
//        OwnUser requester = userTestUtils.generateWithoutPrivilege(companyCreator, PrivilegeEnum.VIEW_SETTINGS);
//        OwnUser targetUser = userTestUtils.generateWithRole(companyCreator,
//                userTestUtils.getRandomRole(companyCreator));
//
//        UserPatchDTO patchDTO = new UserPatchDTO();
//        patchDTO.setFirstName(TestHelper.generateString());
//        patchDTO.setLastName(TestHelper.generateString());
//        patchDTO.setLanguage(Language.EN);
//
//        String expectedMessage = messageSource.getMessage("access_denied", null, requester.getLocale());
//
//        mockMvc.perform(patch("/users/{id}", targetUser.getId())
//                        .headers(getAuthHeaders(requester))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO)))
//                .andExpect(status().isForbidden())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException) {
//                        CustomException ex = (CustomException) result.getResolvedException();
//                        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
//                    } else {
//                        Assertions.fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void getByIdShouldReturnUserWhenUserIsInSameCompany() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//        OwnUser targetUser = userTestUtils.generateWithRole(user, userTestUtils.getRandomRole(user));
//
//        mockMvc.perform(get("/users/{id}", targetUser.getId())
//                        .headers(getAuthHeaders(user))
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(result -> {
//                    String content = result.getResponse().getContentAsString();
//                    UserResponseDTO responseDTO = objectMapper.readValue(content, UserResponseDTO.class);
//                    assertThat(responseDTO.getId()).isEqualTo(targetUser.getId());
//                });
//    }
//
//    @Test
//    public void getByIdShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//        long nonExistentId = TestHelper.generateRandomInt(8);
//
//        mockMvc.perform(get("/users/{id}", nonExistentId)
//                        .headers(getAuthHeaders(user))
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isNotFound())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException ex) {
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
//                    } else {
//                        Assertions.fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void getByIdShouldReturnForbiddenWhenUserIsInDifferentCompany() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//        OwnUser otherCompanyUser = userTestUtils.generateUserAndEnable();
//        String expectedMessage = messageSource.getMessage("access_denied", null, user.getLocale());
//
//        mockMvc.perform(get("/users/{id}", otherCompanyUser.getId())
//                        .headers(getAuthHeaders(user))
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isForbidden())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException) {
//                        CustomException ex = (CustomException) result.getResolvedException();
//                        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
//                    } else {
//                        Assertions.fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void getByIdShouldReturnForbiddenWhenUserIsNotAuthenticated() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable();
//        mockMvc.perform(get("/users/{id}", user.getId())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void patchRoleShouldUpdateUserRoleWhenUserHasPrivilegeAndRoleExists() throws Exception {
//        OwnUser requester = userTestUtils.generateUserAndEnable();
//        OwnUser targetUser = userTestUtils.generateWithRole(requester, userTestUtils.getRandomRole(requester));
//        Role newRole = userTestUtils.getRandomRole(requester, true);
//
//        mockMvc.perform(patch("/users/{id}/role", targetUser.getId())
//                        .headers(getAuthHeaders(requester))
//                        .param("role", newRole.getId().toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(result -> {
//                    String content = result.getResponse().getContentAsString();
//                    UserResponseDTO responseDTO = objectMapper.readValue(content, UserResponseDTO.class);
//                    // Fetch the updated user from the database to ensure persistence
//                    OwnUser updatedUser = userService.findById(targetUser.getId()).orElseThrow();
//                    assertThat(updatedUser.getRoles()).anyMatch(role -> role.getId().equals(newRole.getId()));
//                    assertThat(updatedUser.getDoctorRole().getId()).isEqualTo(newRole.getId());
//                });
//
//    }
//
//    @Test
//    public void patchRoleShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
//        OwnUser requester = userTestUtils.generateUserAndEnable();
//        long nonExistentUserId = TestHelper.generateRandomInt(8);
//        Role existingRole = userTestUtils.getRandomRole(requester, true);
//
//        mockMvc.perform(patch("/users/{id}/role", nonExistentUserId)
//                        .headers(getAuthHeaders(requester))
//                        .param("role", existingRole.getId().toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isNotFound())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException) {
//                        CustomException ex = (CustomException) result.getResolvedException();
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
//                    } else {
//                        Assertions.fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void patchRoleShouldReturnNotFoundWhenRoleDoesNotExist() throws Exception {
//        OwnUser requester = userTestUtils.generateUserAndEnable();
//        OwnUser targetUser = userTestUtils.generateWithRole(requester, userTestUtils.getRandomRole(requester));
//        long nonExistentRoleId = TestHelper.generateRandomInt(8);
//
//        mockMvc.perform(patch("/users/{id}/role", targetUser.getId())
//                        .headers(getAuthHeaders(requester))
//                        .param("role", String.valueOf(nonExistentRoleId))
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isNotFound())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException) {
//                        CustomException ex = (CustomException) result.getResolvedException();
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
//                    } else {
//                        Assertions.fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void patchRoleShouldReturnForbiddenWhenUserDoesNotHaveViewSettingsPrivilege() throws Exception {
//        OwnUser companyCreator = userTestUtils.generateUserAndEnable();
//        OwnUser requester = userTestUtils.generateWithoutPrivilege(companyCreator, PrivilegeEnum.VIEW_SETTINGS);
//        OwnUser targetUser = userTestUtils.generateWithRole(companyCreator,
//                userTestUtils.getRandomRole(companyCreator));
//        Role existingRole = userTestUtils.getRandomRole(requester, true);
//        String expectedMessage = messageSource.getMessage("access_denied", null, requester.getLocale());
//
//        mockMvc.perform(patch("/users/{id}/role", targetUser.getId())
//                        .headers(getAuthHeaders(requester))
//                        .param("role", existingRole.getId().toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isForbidden())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException) {
//                        CustomException ex = (CustomException) result.getResolvedException();
//                        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
//                    } else {
//                        Assertions.fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void patchRoleShouldReturnNotAcceptableWhenSubscriptionLimitIsReached() throws Exception {
//        OwnUser requester = userTestUtils.generateUserAndEnable();
//        Subscription subscription = requester.getCompany().getSubscription();
//        subscription.setUsersCount(0); // Set to 0 to easily trigger the limit
//        subscriptionService.save(subscription);
//        OwnUser targetUser = userTestUtils.generateWithRole(requester, userTestUtils.getRandomRole(requester));
//        Role existingRole = userTestUtils.getRandomRole(requester, true);
//        String expectedMessage = messageSource.getMessage("subscription_users_count_doesnt_allow_operation", null,
//                requester.getLocale());
//
//        mockMvc.perform(patch("/users/{id}/role", targetUser.getId())
//                        .headers(getAuthHeaders(requester))
//                        .param("role", existingRole.getId().toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isNotAcceptable())
//                .andExpect(result -> {
//                    if (result.getResolvedException() instanceof CustomException) {
//                        CustomException ex = (CustomException) result.getResolvedException();
//                        assertThat(ex.getMessage()).isEqualTo(expectedMessage);
//                        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
//                    } else {
//                        Assertions.fail("Expected CustomException was not thrown");
//                    }
//                });
//    }
//
//    @Test
//    public void patchRoleShouldReturnForbiddenForUnauthenticatedUser() throws Exception {
//        OwnUser requester = userTestUtils.generateUserAndEnable();
//        OwnUser targetUser = userTestUtils.generateWithRole(requester, userTestUtils.getRandomRole(requester));
//        Role existingRole = userTestUtils.getRandomRole(requester, true);
//        mockMvc.perform(patch("/users/{id}/role", targetUser.getId())
//                        .param("role", existingRole.getId().toString())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isForbidden());
//    }
//}