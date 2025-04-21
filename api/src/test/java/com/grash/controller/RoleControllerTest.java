//package com.grash.controller;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.grash.CustomPostgresSQLContainer;
//import com.grash.dto.RolePatchDTO;
//import com.grash.model.OwnUser;
//import com.grash.model.Role;
//import com.grash.model.enums.RoleCode;
//import com.grash.service.RoleService;
//import com.grash.utils.RoleTestUtils;
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
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class RoleControllerTest extends CustomPostgresSQLContainer {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private UserTestUtils userTestUtils;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//    @Autowired
//    private RoleService roleService;
//    @Autowired
//    private RoleTestUtils roleTestUtils;
//
//
//    @Test
//    public void getAllRoles_shouldReturnRolesWhenUserHasPrivilege() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//
//        mockMvc.perform(get("/roles")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isOk())
//                .andExpect(result -> {
//                    String content = result.getResponse().getContentAsString();
//                    List<Role> roles = objectMapper.readValue(content,
//                            new TypeReference<>() {
//                            });
//                    assertThat(roles.size()).isGreaterThan(1);
//                    assertTrue(roles.stream().allMatch(role -> role.belongsToCompany(user.getCompany())));
//                    assertTrue(roles.stream().noneMatch(role -> role.getCode().equals(RoleCode.SUPER_ADMIN)));
//                });
//    }
//
//    @Test
//    public void getAllRoles_shouldReturnForbiddenWhenUserHasNoPrivilege() throws Exception {
//        OwnUser companyCreator = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        OwnUser user = userTestUtils.generateWithoutPrivilege(companyCreator, PrivilegeEnum.VIEW_SETTINGS);
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//
//        mockMvc.perform(get("/roles")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void getById_shouldReturnRoleWhenUserHasPrivilege() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        Role role = userTestUtils.getRandomRole(user);
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//
//        MvcResult result = mockMvc.perform(get("/roles/{id}", role.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        String content = result.getResponse().getContentAsString();
//        Role returnedRole = objectMapper.readValue(content, Role.class);
//        assertThat(returnedRole.getId()).isEqualTo(role.getId());
//    }
//
//    @Test
//    public void getById_shouldReturnForbiddenWhenUserHasNoPrivilege() throws Exception {
//        OwnUser companyCreator = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        OwnUser user = userTestUtils.generateWithoutPrivilege(companyCreator, PrivilegeEnum.VIEW_SETTINGS);
//        Role role = userTestUtils.getRandomRole(user);
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//
//        mockMvc.perform(get("/roles/{id}", role.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void getById_shouldReturnForbiddenWhenUserIsInAnotherCompany() throws Exception {
//        OwnUser otherCompanyUser = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        OwnUser user = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        Role role = roleTestUtils.generateRoleOnlyForCompany(otherCompanyUser);
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//
//        mockMvc.perform(get("/roles/{id}", role.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isForbidden());
//    }
//
//
//    @Test
//    public void getById_shouldReturnNotFoundWhenRoleDoesNotExist() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//
//        mockMvc.perform(get("/roles/99999")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    public void patch_shouldUpdateRoleWhenUserHasPrivilege() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        Role role = roleTestUtils.generateRoleOnlyForCompany(user);
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//        RolePatchDTO rolePatchDTO = new RolePatchDTO();
//        rolePatchDTO.setName("Updated Role Name");
//
//        MvcResult result = mockMvc.perform(patch("/roles/{id}", role.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers)
//                        .content(objectMapper.writeValueAsString(rolePatchDTO)))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        String content = result.getResponse().getContentAsString();
//        Role returnedRole = objectMapper.readValue(content, Role.class);
//        assertThat(returnedRole.getName()).isEqualTo("Updated Role Name");
//    }
//
//    @Test
//    public void patch_shouldReturnForbiddenWhenUserHasNoPrivilege() throws Exception {
//        OwnUser companyCreator = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        OwnUser user = userTestUtils.generateWithoutPrivilege(companyCreator, PrivilegeEnum.VIEW_SETTINGS);
//        Role role = roleTestUtils.generateRoleOnlyForCompany(user);
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//        RolePatchDTO rolePatchDTO = new RolePatchDTO();
//        rolePatchDTO.setName("Updated Role Name");
//
//        mockMvc.perform(patch("/roles/{id}", role.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers)
//                        .content(objectMapper.writeValueAsString(rolePatchDTO)))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void patch_shouldReturnNotFoundWhenRoleDoesNotExist() throws Exception {
//        OwnUser user = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//        RolePatchDTO rolePatchDTO = new RolePatchDTO();
//        rolePatchDTO.setName("Updated Role Name");
//
//        mockMvc.perform(patch("/roles/{id}", 99999)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers)
//                        .content(objectMapper.writeValueAsString(rolePatchDTO)))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    public void patch_shouldReturnForbiddenWhenRoleNotCompanySpecific() throws Exception {
//        OwnUser companyCreator = userTestUtils.generateUserAndEnable(TenantType.getRandomDoctorTenantType());
//        OwnUser user = userTestUtils.generateWithoutPrivilege(companyCreator, PrivilegeEnum.VIEW_SETTINGS);
//        Role role = roleService.findByCompany(user.getCompany().getId())
//                .stream().filter(role1 -> role1.getCompanySettings() == null)
//                .findFirst().get();
//        HttpHeaders headers = userTestUtils.getHeaders(user);
//        RolePatchDTO rolePatchDTO = new RolePatchDTO();
//        rolePatchDTO.setName("Updated Role Name");
//
//        mockMvc.perform(patch("/roles/{id}", role.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .headers(headers)
//                        .content(objectMapper.writeValueAsString(rolePatchDTO)))
//                .andExpect(status().isForbidden());
//    }
//}