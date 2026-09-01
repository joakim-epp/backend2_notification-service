package com.backend2.backend2_notificationservice.controller;

import com.backend2.backend2_notificationservice.TestcontainersConfiguration;
import com.backend2.backend2_notificationservice.client.CustomerApi;
import com.backend2.backend2_notificationservice.client.CustomerSummary;
import com.backend2.backend2_notificationservice.repository.NotificationRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationControllerIntegrationTest {

    private static final long CUSTOMER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private CustomerApi customerApi;

    @BeforeEach
    void clear() {
        notificationRepository.deleteAll();
    }

    @Test
    void sendReturns201AndPersists() throws Exception {
        when(customerApi.findById(CUSTOMER_ID))
                .thenReturn(new CustomerSummary(CUSTOMER_ID, "Anna", "Svensson", "anna@example.com"));

        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmation(CUSTOMER_ID, 42)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.recipient").value("anna@example.com"))
                .andExpect(jsonPath("$.message").value(
                        "Hi Anna, your booking #42 is confirmed: 2026-09-10 to 2026-09-12. Welcome to Pensionatet."));

        assertThat(notificationRepository.findAll())
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.getCustomerId()).isEqualTo(CUSTOMER_ID);
                    assertThat(n.getBookingId()).isEqualTo(42L);
                    assertThat(n.getRecipient()).isEqualTo("anna@example.com");
                    assertThat(n.getCreatedAt()).isNotNull();
                });
    }

    @Test
    void missingBookingIdReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "checkIn": "2026-09-10", "checkOut": "2026-09-12"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("bookingId"));

        assertThat(notificationRepository.findAll()).isEmpty();
        verify(customerApi, never()).findById(any());
    }

    @Test
    void unparsableDateReturns400() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "bookingId": 42, "checkIn": "inte-ett-datum", "checkOut": "2026-09-12"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    void unknownCustomerReturns404AndSavesNothing() throws Exception {
        when(customerApi.findById(CUSTOMER_ID)).thenThrow(feignError(404));

        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmation(CUSTOMER_ID, 42)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CUSTOMER_NOT_FOUND"));

        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    void customerWithoutEmailReturns409AndSavesNothing() throws Exception {
        when(customerApi.findById(CUSTOMER_ID))
                .thenReturn(new CustomerSummary(CUSTOMER_ID, "Anna", "Svensson", null));

        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmation(CUSTOMER_ID, 42)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CUSTOMER_HAS_NO_EMAIL"));

        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    void customerServiceDownReturns503AndSavesNothing() throws Exception {
        when(customerApi.findById(CUSTOMER_ID)).thenThrow(feignError(503));

        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmation(CUSTOMER_ID, 42)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.errorCode").value("CUSTOMER_SERVICE_UNAVAILABLE"));

        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    void logIsReadPerCustomerNewestFirst() throws Exception {
        when(customerApi.findById(CUSTOMER_ID))
                .thenReturn(new CustomerSummary(CUSTOMER_ID, "Anna", "Svensson", "anna@example.com"));
        when(customerApi.findById(2L))
                .thenReturn(new CustomerSummary(2L, "Bo", "Berg", "bo@example.com"));

        send(CUSTOMER_ID, 42);
        send(CUSTOMER_ID, 43);
        send(2L, 44);

        mockMvc.perform(get("/api/notifications").param("customerId", "1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bookingId").value(43))
                .andExpect(jsonPath("$[1].bookingId").value(42));
    }

    @Test
    void customerIdZeroReturns400() throws Exception {
        mockMvc.perform(get("/api/notifications").param("customerId", "0").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void withoutTokenReturns401AndSavesNothing() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmation(CUSTOMER_ID, 42)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications").param("customerId", "1"))
                .andExpect(status().isUnauthorized());

        assertThat(notificationRepository.findAll()).isEmpty();
        verify(customerApi, never()).findById(any());
    }

    private void send(long customerId, long bookingId) throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmation(customerId, bookingId)))
                .andExpect(status().isCreated());
    }

    private String confirmation(long customerId, long bookingId) {
        return """
                {"customerId": %d, "bookingId": %d, "checkIn": "2026-09-10", "checkOut": "2026-09-12"}
                """.formatted(customerId, bookingId);
    }

    private FeignException feignError(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/customers/" + CUSTOMER_ID,
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("CustomerApi#findById(Long)", response);
    }
}
