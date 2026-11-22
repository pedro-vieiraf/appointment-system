package com.pedrovieira.appointmentsystem;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OpenApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void publishesApiContractWithoutAuthenticationInDevelopment() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.paths['/auth/login'].post.security").doesNotExist())
        .andExpect(jsonPath("$.paths['/appointments'].get.security[0].bearerAuth").isArray())
        .andExpect(jsonPath("$.paths['/appointments'].get.parameters[*].name",
            hasItems("title", "startDate", "endDate", "page", "size", "sort")));
  }

  @Test
  void servesSwaggerUiWithoutAuthenticationInDevelopment() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk());
  }

  @Test
  void requiresAuthenticationForAppointmentsInDevelopment() throws Exception {
    mockMvc.perform(get("/appointments"))
        .andExpect(status().isForbidden());
  }
}
