package com.example.learning.controller;

import com.example.learning.config.ApiConstants;
import com.example.learning.dto.UserDTO;
import com.example.learning.dto.auth.*;
import com.example.learning.exception.*;
import com.example.learning.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock AuthService authService;
    @Mock CookieService cookieService;
    @Mock UserService userService;
    @Mock PasswordResetService passwordResetService;
    @Mock EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthController authController;

    private static final String BASE = ApiConstants.API_V1 + "/auth";

    @BeforeEach
    void setUp() {
        // Standalone setup — nema Spring context, samo controller + exception handler
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new com.example.learning.exception.GlobalExceptionHandler())
                .build();
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────

    @Test
    void register_withValidRequest_shouldReturn200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setFirstName("Nikola");
        request.setLastName("Test");

        doNothing().when(authService).register(any());

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void register_withInvalidEmail_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setPassword("password123");
        request.setFirstName("Nikola");
        request.setLastName("Test");

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withShortPassword_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("short");
        request.setFirstName("Nikola");
        request.setLastName("Test");

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withDuplicateEmail_shouldReturn409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@test.com");
        request.setPassword("password123");
        request.setFirstName("Nikola");
        request.setLastName("Test");

        doThrow(new ConflictException("User with this email already exists"))
                .when(authService).register(any());

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_shouldReturn200WithAccessToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token");

        when(authService.login(any())).thenReturn(authResponse);
        when(cookieService.createRefreshTokenCookie("refresh-token"))
                .thenReturn(ResponseCookie.from("refreshToken", "refresh-token").build());

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void login_withLockedAccount_shouldReturn423() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@test.com");
        request.setPassword("password123");

        when(authService.login(any()))
                .thenThrow(new AccountLockedException("Account locked"));

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(423));
    }

    @Test
    void login_withMissingFields_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─── REFRESH ─────────────────────────────────────────────────────────────

    @Test
    void refresh_withValidCookie_shouldReturn200WithNewAccessToken() throws Exception {
        AuthResponse authResponse = new AuthResponse("new-access-token", "new-refresh-token");

        when(authService.refreshToken("valid-refresh-token")).thenReturn(authResponse);
        when(cookieService.createRefreshTokenCookie("new-refresh-token"))
                .thenReturn(ResponseCookie.from("refreshToken", "new-refresh-token").build());

        mockMvc.perform(post(BASE + "/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    // ─── LOGOUT ───────────────────────────────────────────────────────────────

    @Test
    void logout_withValidTokens_shouldReturn204() throws Exception {
        when(cookieService.deleteRefreshTokenCookie())
                .thenReturn(ResponseCookie.from("refreshToken", "").build());

        mockMvc.perform(post(BASE + "/logout")
                        .cookie(new Cookie("refreshToken", "refresh-token"))
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent());

        verify(authService).logout("refresh-token", "access-token");
    }

    // ─── /me ──────────────────────────────────────────────────────────────────

    @Test
    void getCurrentUser_shouldReturn200WithUserData() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("test@test.com");

        when(userService.getCurrentUser()).thenReturn(userDTO);

        mockMvc.perform(get(BASE + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }
}