package com.grash.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.CustomPostgresSQLContainer;
import com.grash.dto.*;
import com.grash.model.OwnUser;
import com.grash.model.VerificationToken;
import com.grash.model.enums.Language;
import com.grash.repository.UserRepository;
import com.grash.repository.VerificationTokenRepository;
import com.grash.service.UserService;
import com.grash.utils.Helper;
import com.grash.utils.TestHelper;
import com.grash.utils.UserTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends CustomPostgresSQLContainer {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private VerificationTokenRepository verificationTokenRepository;

    @Captor
    private ArgumentCaptor<VerificationToken> verificationTokenArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> passwordArgumentCaptor;

    @Autowired
    private UserRepository userRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    private UserTestUtils userTestUtils;

    @Autowired
    private ObjectMapper objectMapper;
    @SpyBean
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(verificationTokenRepository);
        verificationTokenRepository.deleteAll();
    }

    @Test
    void testActivateAccountSuccess() throws Exception {
        OwnUser testUser = userTestUtils.generateDisabledUser();
        verify(verificationTokenRepository).save(verificationTokenArgumentCaptor.capture());
        VerificationToken verificationToken = verificationTokenArgumentCaptor.getValue();

        mockMvc.perform(get("/auth/activate-account").param("token", verificationToken.getToken()))
                .andExpect(status().isFound()) // 302 Found
                .andExpect(header().string("Location", frontendUrl + "/account/login"));

        // Assert that the user's account is now enabled
        OwnUser updatedUser = userRepository.findByEmailIgnoreCase(testUser.getEmail()).get();
        assertTrue(updatedUser.isEnabled());
    }

    @Test
    void testActivateAccountInvalidToken() throws Exception {
        OwnUser testUser = userTestUtils.generateDisabledUser();
        mockMvc.perform(get("/auth/activate-account").param("token", TestHelper.generateString()))
                .andExpect(status().isFound()) // 302 Found
                .andExpect(header().string("Location", frontendUrl + "/account/register"));

        // Assert that the user's account is still disabled
        OwnUser updatedUser = userRepository.findByEmailIgnoreCase(testUser.getEmail()).get();
        assertFalse(updatedUser.isEnabled());
    }

    @Test
    void testActivateAccountExpiredToken() throws Exception {
        OwnUser testUser = userTestUtils.generateDisabledUser();
        List<VerificationToken> verificationTokens =
                verificationTokenRepository.findAllVerificationTokenEntityByUser(testUser);
        VerificationToken verificationToken = verificationTokens.get(0);
        verificationToken.setExpiryDate(new Date(System.currentTimeMillis() - 1)); // Expired
        verificationTokenRepository.save(verificationToken);

        mockMvc.perform(get("/auth/activate-account").param("token", verificationToken.getToken()))
                .andExpect(status().isFound()) // 302 Found
                .andExpect(header().string("Location", frontendUrl + "/account/register"));

        // Assert that the user's account is still disabled
        OwnUser updatedUser = userRepository.findByEmailIgnoreCase(testUser.getEmail()).get();
        assertFalse(updatedUser.isEnabled());
    }

    @Test
    public void whoamiShouldReturnUserDetailsWhenAuthenticatedWithEmail() throws Exception {
        OwnUser user = userTestUtils.generateUserAndEnable();
        String jwtToken = userTestUtils.getToken(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        mockMvc.perform(get("/auth/me")
                        .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(user.getLastName()));
    }

    @Test
    public void whoamiShouldReturnForbiddenWhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void whoamiShouldReturnForbiddenWhenTokenIsInvalid() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalidToken");

        mockMvc.perform(get("/auth/me")
                        .headers(headers))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updatePasswordShouldSucceedWithValidOldPassword() throws Exception {
        Pair<OwnUser, String> userPasswordPair = generateUserWithPassword();
        OwnUser user = userPasswordPair.getFirst();
        String oldPassword = userPasswordPair.getSecond();
        String jwtToken = userTestUtils.getToken(user);

        String newPassword = "newPassword";

        UpdatePasswordRequest updatePasswordRequest = new UpdatePasswordRequest();
        updatePasswordRequest.setOldPassword(oldPassword);
        updatePasswordRequest.setNewPassword(newPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        mockMvc.perform(post("/auth/updatepwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePasswordRequest))
                        .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        OwnUser updatedUser = userRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not " +
                "found"));
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()));
    }

    @Test
    public void updatePasswordShouldFailWithInvalidOldPassword() throws Exception {
        Pair<OwnUser, String> userPasswordPair = generateUserWithPassword();
        OwnUser user = userPasswordPair.getFirst();
        String oldPassword = userPasswordPair.getSecond();
        user.setEnabled(true);
        userService.save(user);
        String jwtToken = userTestUtils.getToken(user);

        String incorrectPassword = "wrongPassword";
        String newPassword = "newPassword";

        UpdatePasswordRequest updatePasswordRequest = new UpdatePasswordRequest();
        updatePasswordRequest.setOldPassword(incorrectPassword);
        updatePasswordRequest.setNewPassword(newPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        mockMvc.perform(post("/auth/updatepwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePasswordRequest))
                        .headers(headers))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bad credentials"));

        OwnUser unchangedUser = userRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User " +
                "not found"));
        assertTrue(passwordEncoder.matches(oldPassword, unchangedUser.getPassword()));
    }

    @Test
    public void updatePasswordShouldFailWithoutToken() throws Exception {
        UpdatePasswordRequest updatePasswordRequest = new UpdatePasswordRequest();
        updatePasswordRequest.setOldPassword("oldPassword");
        updatePasswordRequest.setNewPassword("newPassword");

        mockMvc.perform(post("/auth/updatepwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePasswordRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updatePasswordShouldFailWithInvalidToken() throws Exception {
        UpdatePasswordRequest updatePasswordRequest = new UpdatePasswordRequest();
        updatePasswordRequest.setOldPassword("oldPassword");
        updatePasswordRequest.setNewPassword("newPassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalidToken");

        mockMvc.perform(post("/auth/updatepwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePasswordRequest))
                        .headers(headers))
                .andExpect(status().isUnauthorized());
    }


    private Pair<OwnUser, String> generateUserWithPassword() {
        UserSignupRequest userSignupRequest =
                userTestUtils.getRandomSignupRequest();
        String password = userSignupRequest.getPassword();
        userService.signup(userSignupRequest);
        OwnUser user = userService.findByEmail(userSignupRequest.getEmail()).get();
        user.setEnabled(true);
        userService.save(user);
        return Pair.of(user, password);
    }

    @Test
    public void resetPasswordShouldSucceed() throws Exception {
        OwnUser user = userTestUtils.generateUserAndEnable();
        String email = user.getEmail().toLowerCase();

        Mockito.reset(passwordEncoder);
        mockMvc.perform(get("/auth/resetpwd")
                        .param("email", email)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(passwordEncoder).encode(passwordArgumentCaptor.capture());
        String newPassword = passwordArgumentCaptor.getValue();
        OwnUser updatedUser =
                userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new RuntimeException("User not " +
                        "found"));
        assertNotEquals(user.getPassword(), updatedUser.getPassword()); // Ensure password is updated
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()));
    }

    @Test
    public void resetPasswordShouldFailIfEmailDoesNotExist() throws Exception {
        String nonExistentEmail = TestHelper.generateEmail();

        mockMvc.perform(get("/auth/resetpwd")
                        .param("email", nonExistentEmail)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("No value present"));
    }

    @Test
    public void resetPasswordShouldFailIfEmailIsMissing() throws Exception {
        mockMvc.perform(get("/auth/resetpwd")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void signupShouldReturnSuccessResponseAndCreatedUserWhenRequestIsValid() throws Exception {
        UserSignupRequest signupRequest = userTestUtils.getRandomSignupRequest();

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    SuccessResponse response = objectMapper.readValue(content,
                            new TypeReference<SuccessResponse>() {
                            });
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getMessage()).startsWith("Successful");
                    Optional<OwnUser> optionalUserInDb = userService.findByEmail(signupRequest.getEmail());
                    assertTrue(optionalUserInDb.isPresent());
                    OwnUser userInDb = optionalUserInDb.get();
                    assertEquals(signupRequest.getFirstName().toLowerCase(),
                            userInDb.getFirstName().toLowerCase());
                    assertEquals(signupRequest.getEmail().toLowerCase(),
                            userInDb.getEmail().toLowerCase());
                });
    }

    @Test
    public void signupShouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        UserSignupRequest signupRequest = userTestUtils.getRandomSignupRequest();
        signupRequest.setEmail("invalid_email");
        signupRequest.setPassword("password");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void signupShouldReturnBadRequestWhenRequiredFieldsAreMissing() throws Exception {
        UserSignupRequest signupRequest = new UserSignupRequest();
        signupRequest.setPassword("password");
        signupRequest.setFirstName("John");
        signupRequest.setLastName("Doe");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // Login tests
    @Test
    public void loginShouldReturnTokenAndUserDetailsWhenCredentialsAreValid() throws Exception {
        // Arrange
        UserSignupRequest signupRequest = userTestUtils.getRandomSignupRequest();

        userService.signup(signupRequest);
        OwnUser user = userService.findByEmail(signupRequest.getEmail()).get();
        user.setEnabled(true);
        userService.save(user);

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setEmail(signupRequest.getEmail());
        loginRequest.setPassword(signupRequest.getPassword());
        loginRequest.setType("CLIENT");

        // Act
        MvcResult mvcResult = mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse responseBody =
                objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                        new TypeReference<AuthResponse>() {
                        });

        assertThat(responseBody.getAccessToken()).isNotEmpty();
    }

    @Test
    public void loginShouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
        UserSignupRequest signupRequest = userTestUtils.getRandomSignupRequest();

        userService.signup(signupRequest);
        OwnUser user = userService.findByEmail(signupRequest.getEmail()).get();
        user.setEnabled(true);
        userService.save(user);

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setEmail(signupRequest.getEmail());
        loginRequest.setPassword("wrong_password"); // Wrong password
        loginRequest.setType("CLIENT");
        // Act
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginShouldReturnUnauthorizedWhenEmailDoesNotExist() throws Exception {
        // Arrange
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setEmail("non-existent@example.com");
        loginRequest.setPassword("password");
        loginRequest.setType("CLIENT");

        // Act
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

}