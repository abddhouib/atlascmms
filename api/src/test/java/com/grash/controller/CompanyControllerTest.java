package com.grash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.CustomPostgresSQLContainer;
import com.grash.dto.CompanyPatchDTO;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PermissionType;
import com.grash.service.UserService;
import com.grash.utils.UserTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CompanyControllerTest extends CustomPostgresSQLContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserTestUtils userTestUtils;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;

    @Test
    public void getCompanyByIdShouldNotReturnCompanyForPublicAccess() throws Exception {
        OwnUser companyCreator = userTestUtils.generateUserAndEnable();
        Company company = companyCreator.getCompany();

        mockMvc.perform(get("/companies/{id}", company.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getCompanyByIdShouldReturnCompanyForAuthenticatedUsers() throws Exception {
        OwnUser companyCreator = userTestUtils.generateUserAndEnable();
        Company company = companyCreator.getCompany();

        OwnUser otherUser = userTestUtils.generateUserAndEnable();
        HttpHeaders headers = userTestUtils.getHeaders(otherUser);

        MvcResult result = mockMvc.perform(get("/companies/{id}", company.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(status().isOk())
                .andReturn();

        Company companyDto = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Company.class
        );

        assertEquals(company.getId(), companyDto.getId());
        assertEquals(company.getName(), companyDto.getName());
    }

    @Test
    public void getCompanyByIdShouldReturnNotFoundForNonExistentCompany() throws Exception {
        OwnUser user = userTestUtils.generateUserAndEnable();
        HttpHeaders headers = userTestUtils.getHeaders(user);
        mockMvc.perform(get("/companies/{id}", 99999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(status().isNotFound());
    }

    @Test
    public void patchCompanyShouldSucceedWithValidRequest() throws Exception {
        OwnUser companyAdmin = userTestUtils.generateUserAndEnable();
        CompanyPatchDTO companyPatch = new CompanyPatchDTO();
        companyPatch.setName("Updated Company Name");

        HttpHeaders headers = userTestUtils.getHeaders(companyAdmin);

        mockMvc.perform(patch("/companies/{id}", companyAdmin.getCompany().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyPatch))
                        .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(companyPatch.getName()));

        Optional<OwnUser> optionalUser = userService.findById(companyAdmin.getId());
        assertThat(optionalUser).isPresent();
        Company company = optionalUser.get().getCompany();
        assertEquals(company.getName(), companyPatch.getName());
    }

    @Test
    public void patchCompanyShouldReturnForbiddenIfUserLacksPrivilege() throws Exception {
        OwnUser companyUser = userTestUtils.generateUserAndEnable();
        OwnUser user = userTestUtils.generateWithoutPrivilege(companyUser, PermissionType.VIEW,
                PermissionEntity.SETTINGS);

        CompanyPatchDTO companyPatch = new CompanyPatchDTO();
        companyPatch.setName("Updated Company Name");

        HttpHeaders headers = userTestUtils.getHeaders(user);

        mockMvc.perform(patch("/companies/{id}", companyUser.getCompany().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyPatch))
                        .headers(headers))
                .andExpect(status().isForbidden());
    }

    @Test
    public void patchCompanyShouldReturnForbiddenIfUserIsFromDifferentCompany() throws Exception {
        OwnUser companyAdmin = userTestUtils.generateUserAndEnable();
        OwnUser otherCompanyUser = userTestUtils.generateUserAndEnable();

        CompanyPatchDTO companyPatch = new CompanyPatchDTO();
        companyPatch.setName("Updated Company Name");
        HttpHeaders headers = userTestUtils.getHeaders(otherCompanyUser);

        mockMvc.perform(patch("/companies/{id}", companyAdmin.getCompany().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyPatch))
                        .headers(headers))
                .andExpect(status().isForbidden());
    }


    @Test
    public void patchCompanyShouldReturnBadRequestWithInvalidData() throws Exception {
        OwnUser companyAdmin = userTestUtils.generateUserAndEnable();

        String invalidCompanyJson = "{\"name\": null}"; // Empty name

        HttpHeaders headers = userTestUtils.getHeaders(companyAdmin);

        mockMvc.perform(patch("/companies/{id}", companyAdmin.getCompany().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCompanyJson)
                        .headers(headers))
                .andExpect(status().isBadRequest());
    }

}