//package com.grash.controller;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.grash.CustomPostgresSQLContainer;
//import com.grash.dto.SubscriptionPlanPatchDTO;
//import com.grash.dto.SuccessResponse;
//import com.grash.model.OwnUser;
//import com.grash.model.SubscriptionPlan;
//import com.grash.model.enums.PrivilegeEnum;
//import com.grash.model.enums.TenantType;
//import com.grash.repository.SubscriptionPlanRepository;
//import com.grash.service.PrivilegeService;
//import com.grash.service.SubscriptionPlanService;
//import com.grash.utils.Helper;
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
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class SubscriptionPlanControllerTest extends CustomPostgresSQLContainer {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private UserTestUtils userTestUtils;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private SubscriptionPlanService subscriptionPlanService;
//
//    @Autowired
//    private SubscriptionPlanRepository subscriptionPlanRepository;
//    @Autowired
//    private PrivilegeService privilegeService;
//
//    private SubscriptionPlan createTestSubscriptionPlan() {
//        SubscriptionPlan plan = new SubscriptionPlan();
//        plan.setName("Test Plan " + TestHelper.generateString());
//        plan.setMonthlyCostPerUser(1000L);
//        plan.setYearlyCostPerUser(10000L);
//        plan.setCode("TEST_" + TestHelper.generateString());
//        plan.setTenantType(TenantType.getRandomDoctorTenantType());
//
//        plan.setPrivileges(new HashSet<>(privilegeService.getByNameIn(Arrays.asList(PrivilegeEnum.VIEW_SETTINGS
//        .name()))));
//
//        return subscriptionPlanService.create(plan);
//    }
//
//    @Test
//    public void getAllShouldReturnAllSubscriptionPlans() throws Exception {
//        // Create test subscription plans
//        SubscriptionPlan plan1 = createTestSubscriptionPlan();
//        SubscriptionPlan plan2 = createTestSubscriptionPlan();
//
//        // Perform get all request
//        MvcResult result = mockMvc.perform(get("/subscription-plans")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Parse the response
//        String content = result.getResponse().getContentAsString();
//        Collection<SubscriptionPlan> plans = objectMapper.readValue(
//                content,
//                new TypeReference<Collection<SubscriptionPlan>>() {
//                }
//        );
//
//        // Verify results
//        assertFalse(plans.isEmpty());
//        assertTrue(plans.stream()
//                .anyMatch(plan -> plan.getId().equals(plan1.getId())));
//        assertTrue(plans.stream()
//                .anyMatch(plan -> plan.getId().equals(plan2.getId())));
//    }
//
//    @Test
//    public void getByIdShouldReturnSubscriptionPlanForAuthenticatedUser() throws Exception {
//        // Create a user
//        OwnUser user = userTestUtils.generateUserAndEnable(TenantType.CLINIC);
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//
//        // Create a subscription plan
//        SubscriptionPlan plan = createTestSubscriptionPlan();
//
//        // Perform getById request
//        MvcResult result = mockMvc.perform(get("/subscription-plans/{id}", plan.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Parse the response
//        SubscriptionPlan returnedPlan = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                SubscriptionPlan.class
//        );
//
//        // Verify results
//        assertEquals(plan.getId(), returnedPlan.getId());
//        assertEquals(plan.getName(), returnedPlan.getName());
//        assertEquals(plan.getCode(), returnedPlan.getCode());
//        assertEquals(plan.getMonthlyCostPerUser(), returnedPlan.getMonthlyCostPerUser());
//        assertEquals(plan.getYearlyCostPerUser(), returnedPlan.getYearlyCostPerUser());
//    }
//
//    @Test
//    public void getByIdShouldReturnNotFoundForNonExistentPlan() throws Exception {
//        // Create a user
//        OwnUser user = userTestUtils.generateUserAndEnable(TenantType.CLINIC);
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//
//        // Use a non-existent plan ID
//        Long nonExistentId = 99999L;
//
//        // Perform getById request
//        mockMvc.perform(get("/subscription-plans/{id}", nonExistentId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    public void createShouldSucceedForSuperAdmin() throws Exception {
//        // Create a super admin user
//        OwnUser superAdmin = userTestUtils.generateUserAndEnable(TenantType.SUPER_ADMIN);
//        HttpHeaders headers = userTestUtils.getHeaders(superAdmin);
//
//        // Create a new subscription plan
//        SubscriptionPlan newPlan = new SubscriptionPlan();
//        newPlan.setName("New Test Plan " + TestHelper.generateString());
//        newPlan.setMonthlyCostPerUser(2000L);
//        newPlan.setYearlyCostPerUser(20000L);
//        newPlan.setCode("NEW_TEST_" + TestHelper.generateString());
//        newPlan.setTenantType(TenantType.getRandomDoctorTenantType());
//
//        newPlan.setPrivileges(new HashSet<>(privilegeService.getByNameIn(Arrays.asList(PrivilegeEnum.VIEW_SETTINGS
//        .name()))));
//
//        // Perform create request
//        MvcResult result = mockMvc.perform(post("/subscription-plans")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(newPlan))
//                        .headers(headers))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Parse the response
//        SubscriptionPlan createdPlan = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                SubscriptionPlan.class
//        );
//
//        // Verify results
//        assertNotNull(createdPlan.getId());
//        assertEquals(newPlan.getName(), createdPlan.getName());
//        assertEquals(newPlan.getCode(), createdPlan.getCode());
//        assertEquals(newPlan.getMonthlyCostPerUser(), createdPlan.getMonthlyCostPerUser());
//        assertEquals(newPlan.getYearlyCostPerUser(), createdPlan.getYearlyCostPerUser());
//    }
//
//    @Test
//    public void createShouldReturnForbiddenForNonSuperAdmin() throws Exception {
//        // Create a regular user
//        OwnUser regularUser = userTestUtils.generateUserAndEnable(TenantType.CLINIC);
//        HttpHeaders headers = userTestUtils.getHeaders(regularUser);
//
//        // Create a new subscription plan
//        SubscriptionPlan newPlan = new SubscriptionPlan();
//        newPlan.setName("New Test Plan " + TestHelper.generateString());
//        newPlan.setMonthlyCostPerUser(2000L);
//        newPlan.setYearlyCostPerUser(20000L);
//        newPlan.setCode("NEW_TEST_" + TestHelper.generateString());
//        newPlan.setTenantType(TenantType.getRandomDoctorTenantType());
//
//        // Perform create request - should be forbidden
//        mockMvc.perform(post("/subscription-plans")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(newPlan))
//                        .headers(headers))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void patchShouldSucceedForSuperAdmin() throws Exception {
//        // Create a super admin user
//        OwnUser superAdmin = userTestUtils.generateUserAndEnable(TenantType.SUPER_ADMIN);
//        HttpHeaders headers = userTestUtils.getHeaders(superAdmin);
//
//        // Create a subscription plan
//        SubscriptionPlan plan = createTestSubscriptionPlan();
//
//        // Create patch DTO
//        SubscriptionPlanPatchDTO patchDTO = new SubscriptionPlanPatchDTO();
//        patchDTO.setName("Updated Plan Name");
//        patchDTO.setMonthlyCostPerUser(3000L);
//        patchDTO.setYearlyCostPerUser(30000L);
//        patchDTO.setCode("UPDATED_" + TestHelper.generateString());
//
//        // Perform patch request
//        MvcResult result = mockMvc.perform(patch("/subscription-plans/{id}", plan.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO))
//                        .headers(headers))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Parse the response
//        SubscriptionPlan updatedPlan = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                SubscriptionPlan.class
//        );
//
//        // Verify results
//        assertEquals(plan.getId(), updatedPlan.getId());
//        assertEquals(patchDTO.getName(), updatedPlan.getName());
//        assertEquals(patchDTO.getCode(), updatedPlan.getCode());
//        assertEquals(patchDTO.getMonthlyCostPerUser(), updatedPlan.getMonthlyCostPerUser());
//        assertEquals(patchDTO.getYearlyCostPerUser(), updatedPlan.getYearlyCostPerUser());
//    }
//
//    @Test
//    public void patchShouldReturnNotFoundForNonExistentPlan() throws Exception {
//        // Create a super admin user
//        OwnUser superAdmin = userTestUtils.generateUserAndEnable(TenantType.SUPER_ADMIN);
//        HttpHeaders headers = userTestUtils.getHeaders(superAdmin);
//        // Create patch DTO
//        SubscriptionPlanPatchDTO patchDTO = new SubscriptionPlanPatchDTO();
//        patchDTO.setName("Updated Plan Name");
//        patchDTO.setMonthlyCostPerUser(3000L);
//        patchDTO.setYearlyCostPerUser(30000L);
//        patchDTO.setCode("UPDATED_" + TestHelper.generateString());
//
//        // Use a non-existent plan ID
//        Long nonExistentId = 99999L;
//
//        // Perform patch request - should return not found
//        mockMvc.perform(patch("/subscription-plans/{id}", nonExistentId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO))
//                        .headers(headers))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    public void patchShouldReturnForbiddenForNonSuperAdmin() throws Exception {
//        // Create a regular user
//        OwnUser regularUser = userTestUtils.generateUserAndEnable(TenantType.CLINIC);
//        HttpHeaders headers = userTestUtils.getHeaders(regularUser);
//
//        // Create a subscription plan
//        SubscriptionPlan plan = createTestSubscriptionPlan();
//
//        // Create patch DTO
//        SubscriptionPlanPatchDTO patchDTO = new SubscriptionPlanPatchDTO();
//        patchDTO.setName("Updated Plan Name");
//        patchDTO.setMonthlyCostPerUser(3000L);
//        patchDTO.setYearlyCostPerUser(30000L);
//        patchDTO.setCode("UPDATED_" + TestHelper.generateString());
//
//        // Perform patch request - should be forbidden
//        mockMvc.perform(patch("/subscription-plans/{id}", plan.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO))
//                        .headers(headers))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void deleteShouldSucceedForSuperAdmin() throws Exception {
//        // Create a super admin user
//        OwnUser superAdmin = userTestUtils.generateUserAndEnable(TenantType.SUPER_ADMIN);
//        HttpHeaders headers = userTestUtils.getHeaders(superAdmin);
//        // Create a subscription plan
//        SubscriptionPlan plan = createTestSubscriptionPlan();
//
//        // Perform delete request
//        MvcResult result = mockMvc.perform(delete("/subscription-plans/{id}", plan.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Parse the response
//        SuccessResponse response = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                SuccessResponse.class
//        );
//
//        // Verify success response
//        assertTrue(response.isSuccess());
//
//        // Verify plan is deleted
//        Optional<SubscriptionPlan> deletedPlan = subscriptionPlanRepository.findById(plan.getId());
//        assertFalse(deletedPlan.isPresent());
//    }
//
//    @Test
//    public void deleteShouldReturnNotFoundForNonExistentPlan() throws Exception {
//        // Create a super admin user
//        OwnUser superAdmin = userTestUtils.generateUserAndEnable(TenantType.SUPER_ADMIN);
//        // Mock the user as a super admin by setting appropriate headers
//        HttpHeaders headers = userTestUtils.getHeaders(superAdmin);
//        // Use a non-existent plan ID
//        Long nonExistentId = 99999L;
//
//        // Perform delete request - should return not found
//        mockMvc.perform(delete("/subscription-plans/{id}", nonExistentId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    public void deleteShouldReturnForbiddenForNonSuperAdmin() throws Exception {
//        // Create a regular user
//        OwnUser regularUser = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        HttpHeaders headers = userTestUtils.getHeaders(regularUser);
//
//        // Create a subscription plan
//        SubscriptionPlan plan = createTestSubscriptionPlan();
//
//        // Perform delete request - should be forbidden
//        mockMvc.perform(delete("/subscription-plans/{id}", plan.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isForbidden());
//    }
//}