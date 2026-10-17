package com.opsflow.organizations.controller;

import com.opsflow.auth.security.JwtService;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.testsupport.TestDatabaseCleaner;
import com.opsflow.users.domain.Role;
import com.opsflow.users.domain.User;
import com.opsflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    private Organization orgA;
    private Organization orgB;
    private String adminAToken;
    private String employeeAToken;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clean();
        orgA = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        orgB = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));
        adminAToken = jwtService.generateToken(
            userRepository.save(new User(orgA, "admin@alpha.com", "hash", "Admin", "Alpha", Role.ORG_ADMIN)));
        employeeAToken = jwtService.generateToken(
            userRepository.save(new User(orgA, "emp@alpha.com", "hash", "Emp", "Alpha", Role.EMPLOYEE)));
    }

    @Test
    void anyMemberShouldReadTheirOwnOrganization() throws Exception {
        mockMvc.perform(get("/api/v1/organization").header("Authorization", "Bearer " + employeeAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(orgA.getId().toString()))
            .andExpect(jsonPath("$.data.name").value("Alpha Corp"))
            .andExpect(jsonPath("$.data.slug").value("alpha-corp"));

        mockMvc.perform(get("/api/v1/organization"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void orgAdminShouldRenameOnlyTheirOwnOrganization() throws Exception {
        mockMvc.perform(put("/api/v1/organization")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  Alpha Holdings  \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Alpha Holdings"))
            .andExpect(jsonPath("$.data.slug").value("alpha-corp"));

        assertEquals("Alpha Holdings", organizationRepository.findById(orgA.getId()).orElseThrow().getName());
        assertEquals("Beta LLC", organizationRepository.findById(orgB.getId()).orElseThrow().getName());
    }

    @Test
    void renameShouldRequireOrgAdminAndAValidName() throws Exception {
        mockMvc.perform(put("/api/v1/organization")
                .header("Authorization", "Bearer " + employeeAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hijacked\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/organization")
                .header("Authorization", "Bearer " + adminAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \"}"))
            .andExpect(status().isBadRequest());

        assertEquals("Alpha Corp", organizationRepository.findById(orgA.getId()).orElseThrow().getName());
    }
}
