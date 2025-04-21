//package com.grash.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.grash.CustomPostgresSQLContainer;
//import com.grash.model.UserSettings;
//import com.grash.model.OwnUser;
//import com.grash.model.enums.TenantType;
//import com.grash.service.UserSettingsService;
//import com.grash.utils.UserTestUtils;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class NotificationSettingsControllerTest extends CustomPostgresSQLContainer {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private UserTestUtils userTestUtils;
//
//    @Autowired
//    private UserSettingsService userSettingsService;
//
//    @Test
//    public void patchNotificationSettingsShouldSucceedForOwnSettings() throws Exception {
//        // Generate a user with clinic or laboratory role
//        OwnUser clinicUser = userTestUtils.generateUserAndEnable();
//
//        // Get the user's notification preferences
//        UserSettings userPreferences = clinicUser.getUserSettings();
//        assertNotNull(userPreferences, "OwnUser should have notification preferences");
//
//        // Create updated preferences
//        UserSettings updatedPreferences = new UserSettings();
//        // Set properties based on the model's available fields
//        // For example, if there are boolean flags for different notification types:
//        updatedPreferences.setId(userPreferences.getId());
//        updatedPreferences.setEveningSummaryEmail(false);
//        updatedPreferences.setDailyAgendaEmail(false);
//        // Set HTTP headers with authentication
//        HttpHeaders headers = userTestUtils.getHeaders(clinicUser);
//
//        // Perform PATCH request
//        MvcResult result = mockMvc.perform(patch("/user-settings/{id}", userPreferences.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updatedPreferences))
//                        .headers(headers))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Verify response
//        UserSettings responsePreferences = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                UserSettings.class
//        );
//
//        assertEquals(userPreferences.getId(), responsePreferences.getId());
//        assertEquals(updatedPreferences.isEveningSummaryEmail(), responsePreferences.isEveningSummaryEmail());
//        assertEquals(updatedPreferences.isDailyAgendaEmail(), responsePreferences.isDailyAgendaEmail());
//        // Add assertions for other fields that should be updated
//    }
//
//    @Test
//    public void patchNotificationSettingsShouldFailForOtherUserSettings() throws Exception {
//        // Generate two users
//        OwnUser firstUser = userTestUtils.generateUserAndEnable();
//        OwnUser secondUser = userTestUtils.generateUserAndEnable();
//
//        // Get the first user's notification preferences
//        UserSettings firstUserPreferences = firstUser.getUserSettings();
//        assertNotNull(firstUserPreferences, "OwnUser should have notification preferences");
//
//        // Create updated preferences
//        UserSettings updatedPreferences = new UserSettings();
//        updatedPreferences.setId(firstUserPreferences.getId());
//
//        // Set HTTP headers with second user's authentication
//        HttpHeaders headers = userTestUtils.getHeaders(secondUser);
//
//        // Attempt to update first user's preferences with second user's credentials
//        mockMvc.perform(patch("/user-settings/{id}", firstUserPreferences.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updatedPreferences))
//                        .headers(headers))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void patchNotificationSettingsShouldFailForNonExistentSettings() throws Exception {
//        // Generate a user
//        OwnUser user = userTestUtils.generateUserAndEnable();
//
//        // Create preferences with non-existent ID
//        UserSettings updatedPreferences = new UserSettings();
//        Long nonExistentId = 99999L;
//        updatedPreferences.setId(nonExistentId);
//
//        // Set HTTP headers
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//
//        // Attempt to update non-existent preferences
//        mockMvc.perform(patch("/user-settings/{id}", nonExistentId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updatedPreferences))
//                        .headers(headers))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void patchNotificationSettingsShouldRequireClinicOrLaboratoryRole() throws Exception {
//        // Generate a user with a patient role (not clinic or laboratory)
//        OwnUser clinician = userTestUtils.generateUserAndEnable();
//        OwnUser patient = userTestUtils.generatePatient(clinician);
//
//        // Get the clinician's notification preferences to try to update with patient credentials
//        UserSettings preferences = clinician.getUserSettings();
//        assertNotNull(preferences, "Clinician should have notification preferences");
//
//        // Create updated preferences
//        UserSettings updatedPreferences = new UserSettings();
//        updatedPreferences.setId(preferences.getId());
//
//        // Set HTTP headers with patient's authentication
//        HttpHeaders headers = userTestUtils.getHeaders(patient);
//
//        // Attempt to update preferences with patient credentials
//        mockMvc.perform(patch("/user-settings/{id}", preferences.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(updatedPreferences))
//                        .headers(headers))
//                .andExpect(status().isForbidden());
//    }
//}