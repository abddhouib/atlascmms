//package com.grash.service;
//
//import com.grash.CustomPostgresSQLContainer;
//import com.grash.dto.SignupSuccessResponse;
//import com.grash.dto.UserSignupRequest;
//import com.grash.exception.CustomException;
//import com.grash.model.OwnUser;
//import com.grash.model.Role;
//import com.grash.model.enums.BusinessType;
//import com.grash.model.enums.Language;
//import com.grash.model.enums.RoleCode;
//import com.grash.model.enums.TenantType;
//import com.grash.utils.UserTestUtils;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInfo;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.util.Pair;
//
//import java.util.Arrays;
//import java.util.Locale;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//@SpringBootTest
//class AuthTest extends CustomPostgresSQLContainer {
//
//    @Autowired
//    private UserService userService;
//    @Autowired
//    private CountryService countryService;
//    @Autowired
//    private UserTestUtils userTestUtils;
//
//    private UserSignupRequest validSignupRequest;
//
//    @BeforeEach
//    void beforeEach(TestInfo testInfo) {
//        if (testInfo.getTags().contains("signup")) {
//            validSignupRequest = UserSignupRequest.builder()
//                    .email("abc@gmail.com")
//                    .password("1234177")
//                    .firstName("Jean")
//                    .lastName("Dupont")
//                    .phone("12578744")
//                    .companyName("Asperge")
//                    .businessType(BusinessType.CLINIC)
//                    .country(countryService.findByCode("sn").get())
//                    .build();
//        }
//    }
//
//    @Test
//    @Tag("signup")
//    void signupShouldSucceed() {
//        SignupSuccessResponse<OwnUser> response = userService.signup(validSignupRequest);
//
//        assertNull(response.getUser());
//        assertTrue(response.isSuccess());
//
//        Optional<OwnUser> optionalUser = userService.findByEmail(validSignupRequest.getEmail());
//        assertTrue(optionalUser.isPresent());
//        OwnUser user = optionalUser.get();
//        assertEquals(validSignupRequest.getEmail(), user.getEmail());
//        assertEquals(validSignupRequest.getFirstName(), user.getFirstName());
//        assertEquals(validSignupRequest.getLastName(), user.getLastName());
//        assertEquals(validSignupRequest.getCountry(), user.getCompany().getCountry());
//        assertNotNull(user.getCompany().getSubscription());
//        assert user.getRoles().stream().allMatch(role -> Arrays.asList(RoleCode.PATIENT, RoleCode.ADMIN).contains
//        (role.getCode()));
//        assertFalse(user.isEnabled());
//        verify(emailService, times(1)).sendMessageUsingThymeleafTemplate(
//                eq(new String[]{validSignupRequest.getEmail()}), anyString(), anyMap(), anyString(), any(), any());
//    }
//
//    @Test
//    @Tag("signup")
//    void signupShouldFailForInvalidEmailFormat() {
//        validSignupRequest.setEmail("invalid-email");
//
//        assertThrows(CustomException.class, () -> {
//            userService.signup(validSignupRequest);
//        });
//    }
//
//    @Test
//    @Tag("signup")
//    void signupShouldFailForMissingRequiredFields() {
//        validSignupRequest.setEmail(null);
//
//        assertThrows(Exception.class, () -> {
//            userService.signup(validSignupRequest);
//        });
//    }
//
//    @Test
//    @Tag("signup")
//    void signupShouldFailForExistingPhone() {
//        assertThrows(CustomException.class, () -> {
//            userService.signup(validSignupRequest);
//        });
//    }
//
//
//    @Test
//    @Tag("signup")
//    void signupWithRoleShouldSucceed() {
//        OwnUser inviter = userService.findByEmail(validSignupRequest.getEmail()).get();
//        Role role = inviter.getClinicRole();
//        validSignupRequest.setEmail("ahuir@fma.com");
//        validSignupRequest.setPhone("78511102");
//        userService.invite(validSignupRequest.getEmail(), role, inviter);
//        validSignupRequest.setRole(role);
//
//        SignupSuccessResponse<OwnUser> response = userService.signup(validSignupRequest);
//
//        assertNotNull(response.getUser());
//        assertTrue(response.isSuccess());
//        Optional<OwnUser> optionalUserInDb = userService.findByEmail(validSignupRequest.getEmail());
//        assertTrue(optionalUserInDb.isPresent());
//        OwnUser userInDb = optionalUserInDb.get();
//        assertEquals(userInDb.getRoles().stream().filter(role1 -> role1.getId().equals(role.getId())).count(), 1);
//        OwnUser user = response.getUser();
//        assertEquals(validSignupRequest.getEmail(), user.getEmail());
//        assertEquals(validSignupRequest.getFirstName(), user.getFirstName());
//        assertEquals(validSignupRequest.getLastName(), user.getLastName());
//        assertEquals(validSignupRequest.getCountry(), user.getCompany().getCountry());
//        verify(emailService, times(1)).sendMessageUsingThymeleafTemplate(
//                eq(new String[]{validSignupRequest.getEmail()}), anyString(), anyMap(), anyString(), any(), any());
//    }
//
//    @Test
//    @Tag("signin")
//    void shouldNotAllowSignInWhenUserIsNotEnabled() {
//        UserSignupRequest userSignupRequest = userTestUtils.getRandomSignupRequest(TenantType.CLINIC);
//        userService.signup(userSignupRequest, Language.EN);
//
//        assertThrows(CustomException.class, () ->
//                userService.signin(userSignupRequest.getEmail(),
//                        userSignupRequest.getPassword(),
//                        TenantType.CLINIC.name(),
//                        Locale.getDefault()));
//    }
//
//    @Test
//    @Tag("signin")
//    void shouldNotAllowSignInWithWrongTenantType() {
//        UserSignupRequest userSignupRequest = userTestUtils.getRandomSignupRequest(TenantType.CLINIC);
//        userService.signup(userSignupRequest, Language.EN);
//        OwnUser user = userService.findByEmail(userSignupRequest.getEmail()).get();
//        user.setEnabled(true);
//        userService.save(user);
//
//        assertThrows(CustomException.class, () ->
//                userService.signin(userSignupRequest.getEmail(),
//                        userSignupRequest.getPassword(),
//                        TenantType.LABORATORY.name(),
//                        Locale.getDefault()));
//    }
//
//    @Test
//    @Tag("signin")
//    void shouldSignInSuccessfully() {
//        UserSignupRequest userSignupRequest = userTestUtils.getRandomSignupRequest(TenantType.CLINIC);
//        userService.signup(userSignupRequest, Language.EN);
//        OwnUser user = userService.findByEmail(userSignupRequest.getEmail()).get();
//        user.setEnabled(true);
//        userService.save(user);
//
//        Pair<String, OwnUser> tokenAndUser = userService.signin(userSignupRequest.getEmail(),
//                userSignupRequest.getPassword(),
//                TenantType.CLINIC.name(),
//                Locale.getDefault());
//        OwnUser signedInUser = tokenAndUser.getSecond();
//
//        assertEquals(userSignupRequest.getEmail(), signedInUser.getEmail());
//        assertEquals(userSignupRequest.getPhone(), signedInUser.getPhone());
//        assertEquals(TenantType.CLINIC, signedInUser.getCompany().getType());
//    }
//
//}
