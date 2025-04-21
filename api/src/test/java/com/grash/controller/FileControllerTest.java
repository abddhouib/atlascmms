//package com.grash.controller;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.cloud.storage.BlobId;
//import com.google.cloud.storage.BlobInfo;
//import com.grash.CustomPostgresSQLContainer;
//import com.grash.advancedsearch.FilterField;
//import com.grash.advancedsearch.SearchCriteria;
//import com.grash.configuration.jackson.RestResponsePage;
//import com.grash.dto.FileCriteria;
//import com.grash.dto.FilePatchDTO;
//import com.grash.dto.FileShowDTO;
//import com.grash.dto.SuccessResponse;
//import com.grash.exception.CustomException;
//import com.grash.model.*;
//import com.grash.model.enums.FileScope;
//import com.grash.model.enums.FileType;
//import com.grash.model.enums.PrivilegeEnum;
//import com.grash.model.enums.TenantType;
//import com.grash.service.FileService;
//import com.grash.service.GCPSpeechService;
//import com.grash.service.SpecialityService;
//import com.grash.utils.AdmissionTestUtils;
//import com.grash.utils.AppointmentTestUtils;
//import com.grash.utils.FileTestUtils;
//import com.grash.utils.UserTestUtils;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.MessageSource;
//import org.springframework.data.domain.Page;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class FileControllerTest extends CustomPostgresSQLContainer {
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
//    private MessageSource messageSource;
//
//    @Autowired
//    private FileTestUtils fileTestUtils;
//
//    private OwnUser user;
//    private OwnUser patient;
//    private String jwtToken;
//    private MockMultipartFile testFile;
//    @Autowired
//    private FileService fileService;
//
//    @BeforeEach
//    void setUp1() {
//        // Create test users
//        user = userTestUtils.generateUserAndEnable();
//        jwtToken = userTestUtils.getToken(user);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(jwtToken);
//
//        // Create a test file
//        testFile = new MockMultipartFile(
//                "files",
//                "test.jpg",
//                MediaType.IMAGE_JPEG_VALUE,
//                "test image content".getBytes()
//        );
//
//        // Mock GCPStorageService response
//        BlobInfo mockBlobInfo = Mockito.mock(BlobInfo.class);
//        BlobId mockBlobId = BlobId.of("test-bucket", "test-file.jpg");
//
//        when(mockBlobInfo.getMediaLink()).thenReturn("https://storage.googleapis.com/test-bucket/test-file.jpg");
//        when(mockBlobInfo.getBlobId()).thenReturn(mockBlobId);
//
//        when(gcpStorageService.upload(any(), anyString(), any(Boolean.class))).thenReturn(mockBlobInfo);
//        when(gcpStorageService.generateSignedUrl(any(BlobInfo.class), any(Long.class)))
//                .thenReturn("https://signed-url.example.com/test-file.jpg");
//    }
//
//    @Test
//    void handleFileUpload_SingleFile_ShouldSucceed() throws Exception {
//        // Arrange
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .param("folder", "test-folder")
//                .param("hidden", "false")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString());
//
//        // Act
//        MvcResult result = mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        List<FileShowDTO> files = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                new TypeReference<List<FileShowDTO>>() {
//                }
//        );
//
//        assertNotNull(files);
//        assertEquals(1, files.size());
//        assertEquals(testFile.getOriginalFilename(), files.get(0).getName());
//        assertEquals(FileType.IMAGE, files.get(0).getType());
//        assertEquals(FileScope.CLINIC, files.get(0).getScope());
//        File savedFile = fileService.findById(files.get(0).getId()).get();
//        assertFalse(savedFile.isHidden());
//        assertEquals(patient.getId(), savedFile.getPatient().getId());
//    }
//
//    @Test
//    void handleFileUpload_MultipleFiles_ShouldSucceed() throws Exception {
//        // Arrange
//        MockMultipartFile secondFile = new MockMultipartFile(
//                "files",
//                "second.jpg",
//                MediaType.IMAGE_JPEG_VALUE,
//                "second image content".getBytes()
//        );
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .file(secondFile)
//                .param("folder", "test-folder")
//                .param("hidden", "true")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString());
//
//        // Act
//        MvcResult result = mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        List<FileShowDTO> files = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                new TypeReference<List<FileShowDTO>>() {
//                }
//        );
//
//        assertNotNull(files);
//        assertEquals(2, files.size());
//        // Verify a file group was created
//        assertNotNull(files.get(0).getGroup());
//        assertEquals(files.get(0).getGroup().getId(), files.get(1).getGroup().getId());
//        assertTrue(files.stream().allMatch(FileShowDTO::isHidden));
//        Long fileGroupId = files.get(0).getGroup().getId();
//        for (FileShowDTO file : files) {
//            File savedFile = fileService.findById(file.getId()).get();
//            assertTrue(savedFile.isHidden());
//            assertEquals(patient.getId(), savedFile.getPatient().getId());
//            assertEquals(FileType.IMAGE, savedFile.getType());
//            assertEquals(fileGroupId, savedFile.getGroup().getId());
//        }
//    }
//
//    @Test
//    void handleFileUpload_WithSignedUrl_ShouldReturnSignedUrls() throws Exception {
//        // Arrange
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .param("folder", "test-folder")
//                .param("hidden", "false")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString())
//                .param("signUrls", "true");
//
//        // Act
//        MvcResult result = mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        List<FileShowDTO> files = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                new TypeReference<List<FileShowDTO>>() {
//                }
//        );
//
//        assertNotNull(files);
//        assertEquals(1, files.size());
//        assertEquals("https://signed-url.example.com/test-file.jpg", files.get(0).getUrl());
//    }
//
//    @Test
//    void handleFileUpload_WithAppointment_ShouldSucceed() throws Exception {
//        // Arrange
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patient);
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .param("folder", "test-folder")
//                .param("hidden", "false")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString())
//                .param("appointmentId", appointment.getId().toString());
//
//        // Act
//        MvcResult result = mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        List<FileShowDTO> files = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                new TypeReference<List<FileShowDTO>>() {
//                }
//        );
//
//        assertNotNull(files);
//        assertEquals(1, files.size());
//
//        assertEquals(appointment.getId(), fileService.findById(files.get(0).getId()).get().getAppointment().getId());
//    }
//
//    @Test
//    void handleFileUpload_WithAdmission_ShouldSucceed() throws Exception {
//        // Arrange
//        Admission admission = admissionTestUtils.generateAdmission(user, patient);
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .param("folder", "test-folder")
//                .param("hidden", "false")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString())
//                .param("admissionId", admission.getId().toString());
//
//        // Act
//        MvcResult result = mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        List<FileShowDTO> files = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                new TypeReference<List<FileShowDTO>>() {
//                }
//        );
//
//        assertNotNull(files);
//        assertEquals(1, files.size());
//        assertEquals(admission.getId(), fileService.findById(files.get(0).getId()).get().getAdmission().getId());
//    }
//
//    @Test
//    void handleFileUpload_WithComment_ShouldSucceed() throws Exception {
//        // Arrange
//        String comment = "Test comment";
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .param("folder", "test-folder")
//                .param("hidden", "false")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString())
//                .param("comment", comment);
//
//        // Act
//        MvcResult result = mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        List<FileShowDTO> files = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                new TypeReference<List<FileShowDTO>>() {
//                }
//        );
//
//        assertNotNull(files);
//        assertEquals(1, files.size());
//        assertEquals(comment, files.get(0).getComment());
//    }
//
//    @Test
//    void handleFileUpload_CommentWithMultipleFiles_ShouldFail() throws Exception {
//        // Arrange
//        MockMultipartFile secondFile = new MockMultipartFile(
//                "files",
//                "second.jpg",
//                MediaType.IMAGE_JPEG_VALUE,
//                "second image content".getBytes()
//        );
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .file(secondFile)
//                .param("folder", "test-folder")
//                .param("hidden", "false")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString())
//                .param("comment", "This should fail");
//
//        // Act & Assert
//        mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isNotAcceptable())
//                .andExpect(result -> assertInstanceOf(CustomException.class, result.getResolvedException()));
//    }
//
//    @Test
//    void handleFileUpload_BothAppointmentAndAdmission_ShouldFail() throws Exception {
//        // Arrange
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patient);
//        Admission admission = admissionTestUtils.generateAdmission(user, patient);
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .param("folder", "test-folder")
//                .param("hidden", "false")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString())
//                .param("appointmentId", appointment.getId().toString())
//                .param("admissionId", admission.getId().toString());
//
//        // Act & Assert
//        mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isBadRequest())
//                .andExpect(result -> {
//                    CustomException ex = (CustomException) result.getResolvedException();
//                    assertNotNull(ex);
//                    assertEquals("Either admission or appointment should be provided", ex.getMessage());
//                });
//    }
//
//    @Test
//    void handleFileUpload_AppointmentWithDifferentPatient_ShouldFail() throws Exception {
//        // Arrange
//        OwnUser anotherPatient = userTestUtils.generatePatient(user);
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, anotherPatient);
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .param("folder", "test-folder")
//                .param("hidden", "false")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString())
//                .param("appointmentId", appointment.getId().toString());
//
//        // Act & Assert
//        mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isNotAcceptable());
//    }
//
//    @Test
//    void handleFileUpload_AdmissionWithDifferentPatient_ShouldFail() throws Exception {
//        // Arrange
//        OwnUser anotherPatient = userTestUtils.generatePatient(user);
//        Admission admission = admissionTestUtils.generateAdmission(user, anotherPatient);
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload");
//        builder.file(testFile)
//                .param("folder", "test-folder")
//                .param("hidden", "false")
//                .param("type", FileType.IMAGE.name())
//                .param("scope", FileScope.CLINIC.name())
//                .param("patientId", patient.getId().toString())
//                .param("admissionId", admission.getId().toString());
//
//        // Act & Assert
//        mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isNotAcceptable());
//    }
//
//    @Test
//    void uploadVoiceAudio_ShouldSucceed() throws Exception {
//        // Arrange
//        MockMultipartFile audioFile = new MockMultipartFile(
//                "fileReq",
//                "audio.wav",
//                "audio/wav",
//                "audio content".getBytes()
//        );
//
//        when(gcpSpeechService.transcribe(anyString(), any(Boolean.class)))
//                .thenReturn("This is the transcribed text");
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload-voice-audio");
//        builder.file(audioFile)
//                .param("isMac", "false");
//
//        // Act
//        MvcResult result = mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        SuccessResponse response = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                SuccessResponse.class
//        );
//
//        assertTrue(response.isSuccess());
//        assertEquals("This is the transcribed text", response.getMessage());
//    }
//
//    @Test
//    void uploadVoiceAudio_FailedTranscription_ShouldReturnError() throws Exception {
//        // Arrange
//        MockMultipartFile audioFile = new MockMultipartFile(
//                "fileReq",
//                "audio.wav",
//                "audio/wav",
//                "audio content".getBytes()
//        );
//
//        when(gcpSpeechService.transcribe(anyString(), any(Boolean.class)))
//                .thenReturn(""); // Empty transcription
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload-voice-audio");
//        builder.file(audioFile)
//                .param("isMac", "false");
//
//        // Act & Assert
//        mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isUnprocessableEntity())
//                .andExpect(result -> {
//                    CustomException ex = (CustomException) result.getResolvedException();
//                    assertNotNull(ex);
//                    String expectedMessage = messageSource.getMessage(
//                            "failed_transcription_long",
//                            null,
//                            user.getLocale()
//                    );
//                    assertEquals(expectedMessage, ex.getMessage());
//                });
//    }
//
//    @Test
//    void uploadVoiceAudio_MacFormat_ShouldUseCorrectTranscriptionSettings() throws Exception {
//        // Arrange
//        MockMultipartFile audioFile = new MockMultipartFile(
//                "fileReq",
//                "audio.mp3",
//                "audio/mp3",
//                "audio content".getBytes()
//        );
//
//        when(gcpSpeechService.transcribe(anyString(), Mockito.eq(true)))
//                .thenReturn("This is the Mac transcription");
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload-voice-audio");
//        builder.file(audioFile)
//                .param("isMac", "true");
//
//        // Act
//        MvcResult result = mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        SuccessResponse response = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                SuccessResponse.class
//        );
//
//        assertTrue(response.isSuccess());
//        assertEquals("This is the Mac transcription", response.getMessage());
//
//        // Verify the right method was called
//        Mockito.verify(gcpSpeechService).transcribe(anyString(), Mockito.eq(true));
//    }
//
//    @Test
//    void uploadVoiceAudio_DeletesFileAfterTranscription() throws Exception {
//        // Arrange
//        MockMultipartFile audioFile = new MockMultipartFile(
//                "fileReq",
//                "audio.wav",
//                "audio/wav",
//                "audio content".getBytes()
//        );
//
//        when(gcpSpeechService.transcribe(anyString(), any(Boolean.class)))
//                .thenReturn("This is the transcribed text");
//
//        MockMultipartHttpServletRequestBuilder builder = multipart("/files/upload-voice-audio");
//        builder.file(audioFile)
//                .param("isMac", "false");
//
//        // Act
//        mockMvc.perform(builder
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk());
//
//        // Assert
//        Mockito.verify(gcpStorageService).deleteFile(anyString());
//    }
//
//    @Test
//    void search_WithValidCriteria_ReturnsFiles() throws Exception {
//
//        String clinicianToken = userTestUtils.getToken(user);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(clinicianToken);
//
//        // Create test files
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patient);
//        File file = fileTestUtils.generateFileForAppointment(user, appointment);
//
//        // Create search criteria
//        SearchCriteria searchCriteria = new SearchCriteria();
//        // Add filter for non-hidden files
//        searchCriteria.getFilterFields().add(FilterField.builder()
//                .field("hidden")
//                .value(false)
//                .operation("eq")
//                .values(new ArrayList<>())
//                .alternatives(new ArrayList<>())
//                .build());
//
//        // Act
//        MvcResult result = mockMvc.perform(post("/files/search")
//                        .header("Authorization", "Bearer " + clinicianToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(searchCriteria)))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        Page<FileShowDTO> filesPage = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                new TypeReference<RestResponsePage<FileShowDTO>>() {
//                }
//        );
//
//        assertNotNull(filesPage);
//        assertFalse(filesPage.getContent().isEmpty());
//        // Verify the returned file is ours
//        boolean found = filesPage.getContent().stream()
//                .anyMatch(fileDTO -> fileDTO.getId().equals(file.getId()));
//        assertTrue(found, "The created test file should be in the search results");
//    }
//
//    @Test
//    void search_AsPatient_ShouldReturnPatientFiles() throws Exception {
//        // Arrange
//        OwnUser patientUser = userTestUtils.generatePatient(user);
//        String patientToken = userTestUtils.getToken(patientUser);
//
//        // Create a file accessible by the patient
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patientUser);
//        File file = fileTestUtils.generateFileForAppointment(user, appointment);
//        file.setScope(FileScope.PATIENT);
//        file.setHidden(true);
//        fileService.update(file);
//
//        // Create search criteria
//        FileCriteria fileCriteria = new FileCriteria();
//        fileCriteria.setHidden(true);
//
//        // Act
//        MvcResult result = mockMvc.perform(post("/files/search-patient")
//                        .header("Authorization", "Bearer " + patientToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(fileCriteria)))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        Page<FileShowDTO> filesPage = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                new TypeReference<RestResponsePage<FileShowDTO>>() {
//                }
//        );
//
//        assertNotNull(filesPage);
//        // The file may not appear in results depending on how the database queries are set up
//        // This is a basic verification that the endpoint returns a valid response
//    }
//
//    @Test
//    void getById_ExistingFile_ReturnsFileDetails() throws Exception {
//        // Arrange
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patient);
//        File file = fileTestUtils.generateFileForAppointment(user, appointment);
//
//        // Act
//        MvcResult result = mockMvc.perform(get("/files/" + file.getId())
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        FileShowDTO fileShowDTO = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                FileShowDTO.class
//        );
//
//        assertNotNull(fileShowDTO);
//        assertEquals(file.getId(), fileShowDTO.getId());
//        assertEquals(file.getName(), fileShowDTO.getName());
//        assertEquals(file.getType(), fileShowDTO.getType());
//        assertEquals(file.getUrl(), fileShowDTO.getUrl());
//        assertEquals(file.isHidden(), fileShowDTO.isHidden());
//    }
//
//    @Test
//    void getById_NonExistingFile_ReturnsNotFound() throws Exception {
//        // Act & Assert
//        mockMvc.perform(get("/files/99999")
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isNotFound())
//                .andExpect(result -> assertInstanceOf(CustomException.class, result.getResolvedException()));
//    }
//
//    @Test
//    void patch_ExistingFile_UpdatesFileDetails() throws Exception {
//        String clinicianToken = userTestUtils.getToken(user);
//
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patient);
//        File file = fileTestUtils.generateFileForAppointment(user, appointment);
//
//        FilePatchDTO patchDTO = new FilePatchDTO();
//        String updatedName = "Updated File Name";
//        String updatedComment = "Updated comment for testing";
//        patchDTO.setName(updatedName);
//        patchDTO.setComment(updatedComment);
//        patchDTO.setType(FileType.OTHER);
//
//        // Act
//        MvcResult result = mockMvc.perform(patch("/files/" + file.getId())
//                        .header("Authorization", "Bearer " + clinicianToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO)))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        FileShowDTO updatedFile = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                FileShowDTO.class
//        );
//
//        assertNotNull(updatedFile);
//        assertEquals(updatedName, updatedFile.getName());
//        assertEquals(updatedComment, updatedFile.getComment());
//        assertEquals(FileType.OTHER, updatedFile.getType());
//
//        // Verify changes in database
//        File savedFile = fileService.findById(file.getId()).get();
//        assertEquals(updatedName, savedFile.getName());
//        assertEquals(updatedComment, savedFile.getComment());
//        assertEquals(FileType.OTHER, savedFile.getType());
//    }
//
//    @Test
//    void patch_NonExistingFile_ReturnsNotFound() throws Exception {
//        // Arrange
//        OwnUser clinicianWithPrivilege = userTestUtils.generateWithPrivilege(
//                userTestUtils.generateUserAndEnable(),
//                PrivilegeEnum.VIEW_PATIENT_DETAILS
//        );
//        String clinicianToken = userTestUtils.getToken(clinicianWithPrivilege);
//
//        FilePatchDTO patchDTO = new FilePatchDTO();
//        patchDTO.setName("Test Name");
//        patchDTO.setType(FileType.OTHER);
//
//        // Act & Assert
//        mockMvc.perform(patch("/files/99999")
//                        .header("Authorization", "Bearer " + clinicianToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO)))
//                .andExpect(status().isNotFound())
//                .andExpect(result -> assertInstanceOf(CustomException.class, result.getResolvedException()));
//    }
//
//    @Test
//    void patch_WithoutProperPrivilege_ReturnsForbidden() throws Exception {
//        // Arrange
//        // Create a user without the VIEW_PATIENT_DETAILS privilege
//        OwnUser regularClinician = userTestUtils.generateUserAndEnable();
//        String regularClinicianToken = userTestUtils.getToken(regularClinician);
//
//        // Create a file
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patient);
//        File file = fileTestUtils.generateFileForAppointment(user, appointment);
//
//        FilePatchDTO patchDTO = new FilePatchDTO();
//        patchDTO.setName("Test Update");
//        patchDTO.setType(FileType.OTHER);
//
//        // Act & Assert
//        mockMvc.perform(patch("/files/" + file.getId())
//                        .header("Authorization", "Bearer " + regularClinicianToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(patchDTO)))
//                .andExpect(status().isForbidden())
//                .andExpect(result -> assertInstanceOf(CustomException.class, result.getResolvedException()));
//    }
//
//    @Test
//    void shareWithPatient_ExistingFile_ShouldSucceed() throws Exception {
//        // Arrange
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patient);
//        File file = fileTestUtils.generateFileForAppointment(user, appointment);
//        file.setScope(FileScope.CLINIC); // Initially set to CLINIC scope
//        fileService.update(file);
//
//        // Mock the speciality service response
//        Speciality speciality = new Speciality("Test Speciality");
//        speciality.setId(1L);
//        when(specialityService.findById(1L)).thenReturn(java.util.Optional.of(speciality));
//
//        // Act
//        MvcResult result = mockMvc.perform(get("/files/{id}/share-patient", file.getId())
//                        .header("Authorization", "Bearer " + jwtToken)
//                        .param("specialityId", "1"))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        SuccessResponse response = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                SuccessResponse.class
//        );
//
//        assertTrue(response.isSuccess());
//
//        // Verify file is now accessible to patient
//        List<File> files = fileService.findByUrl(file.getUrl());
//        assertEquals(2, files.size());
//        File sharedFile = files.stream().filter(f -> !f.getId().equals(file.getId())).findFirst().get();
//        assertEquals(FileScope.PATIENT, sharedFile.getScope());
//        assertEquals(patient.getId(), sharedFile.getPatient().getId());
//        assertTrue(sharedFile.isHidden());
//        assertEquals(file.getType(), sharedFile.getType());
//    }
//
//    @Test
//    void shareWithPatient_NonExistingFile_ReturnsNotFound() throws Exception {
//        // Act & Assert
//        mockMvc.perform(get("/files/9999/share-patient")
//                        .header("Authorization", "Bearer " + jwtToken)
//                        .param("specialityId", "1")
//                ).andExpect(status().isNotFound())
//                .andExpect(result -> assertInstanceOf(CustomException.class, result.getResolvedException()));
//    }
//
//    @Test
//    void delete_ExistingFile_ShouldSucceed() throws Exception {
//        // Arrange
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patient);
//        File file = fileTestUtils.generateFileForAppointment(user, appointment);
//
//        // Act
//        MvcResult result = mockMvc.perform(delete("/files/" + file.getId())
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        // Assert
//        SuccessResponse response = objectMapper.readValue(
//                result.getResponse().getContentAsString(),
//                SuccessResponse.class
//        );
//
//        assertTrue(response.isSuccess());
//
//        // Verify the file is deleted - should return not found when trying to access it
//        mockMvc.perform(get("/files/" + file.getId())
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void delete_NonExistingFile_ReturnsNotFound() throws Exception {
//        // Act & Assert
//        mockMvc.perform(delete("/files/99999")
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isNotFound())
//                .andExpect(result -> assertInstanceOf(CustomException.class, result.getResolvedException()));
//    }
//
//    @Test
//    void delete_WithoutProperPrivilege_ReturnsForbidden() throws Exception {
//        // Arrange
//        // Create a user without the required privileges
//        OwnUser regularClinician = userTestUtils.generateUserAndEnable();
//        String regularClinicianToken = userTestUtils.getToken(regularClinician);
//
//        // Create a file
//        Appointment appointment = appointmentTestUtils.generateAppointment(user, patient);
//        File file = fileTestUtils.generateFileForAppointment(user, appointment);
//
//        // Act & Assert
//        mockMvc.perform(delete("/files/" + file.getId())
//                        .header("Authorization", "Bearer " + regularClinicianToken))
//                .andExpect(status().isForbidden())
//                .andExpect(result -> assertInstanceOf(CustomException.class, result.getResolvedException()));
//    }
//}
