package com.pedrovieira.appointmentsystem;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
  void documentsActualResponseCodesAndTransitionRules() throws Exception {
    String appointments = "$.paths['/appointments']";
    String confirm = "$.paths['/appointments/{id}/confirm'].patch";
    String cancel = "$.paths['/appointments/{id}/cancel'].patch";
    String complete = "$.paths['/appointments/{id}/complete'].patch";
    String errorSchema = ".content['application/json'].schema['$ref']";

    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/auth/login'].post.responses.keys()",
            containsInAnyOrder("200", "400", "401")))
        .andExpect(jsonPath(appointments + ".post.responses.keys()",
            containsInAnyOrder("201", "400", "403", "409")))
        .andExpect(jsonPath("$.paths['/appointments/{id}'].delete.responses['204'].content")
            .doesNotExist())
        .andExpect(jsonPath(confirm + ".description").value("Changes PENDING to CONFIRMED."))
        .andExpect(jsonPath(cancel + ".description")
            .value("Changes PENDING or CONFIRMED to CANCELLED."))
        .andExpect(jsonPath(complete + ".description").value("Changes CONFIRMED to COMPLETED."))
        .andExpect(jsonPath(confirm + ".responses['409']" + errorSchema)
            .value("#/components/schemas/ErrorResponse"))
        .andExpect(jsonPath(confirm + ".responses['403'].content").doesNotExist());
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
