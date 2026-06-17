package com.bobbo01.supplyhub.global.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void redirectsGetRequestsToRefererWithAlertMessage() throws Exception {
        mockMvc.perform(get("/test/illegal-state")
                        .header("Referer", "http://localhost/workspace?section=commerce"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workspace?section=commerce"))
                .andExpect(flash().attribute("globalAlertMessage", "요청을 처리할 수 없는 상태입니다."));
    }

    @Test
    void redirectsToRootWhenRefererIsMissing() throws Exception {
        mockMvc.perform(post("/test/illegal-argument"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("globalAlertMessage", "입력값을 다시 확인해 주세요."));
    }

    @Test
    void returnsJsonForAjaxRequests() throws Exception {
        mockMvc.perform(get("/test/not-found")
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("요청한 정보를 찾을 수 없습니다."));
    }

    @Test
    void redirectsMissingGetPageToRootWithFixedAlertMessage() throws Exception {
        mockMvc.perform(get("/test/missing-page"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("globalAlertMessage", "존재하지 않은 페이지입니다."));
    }

    @RestController
    @RequestMapping("/test")
    static class ThrowingController {

        @GetMapping("/illegal-state")
        void illegalState() {
            throw new IllegalStateException("internal state message");
        }

        @PostMapping("/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("internal argument message");
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "internal not found message");
        }

        @GetMapping("/missing-page")
        void missingPage() throws NoHandlerFoundException {
            throw new NoHandlerFoundException("GET", "/missing-page", HttpHeaders.EMPTY);
        }
    }
}
