package com.bobbo01.supplyhub.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ALERT_MESSAGE_ATTRIBUTE = "globalAlertMessage";
    private static final String FALLBACK_REDIRECT_PATH = "/";
    private static final String NOT_FOUND_PAGE_MESSAGE = "존재하지 않은 페이지입니다.";

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return handleException(ex, request, response, HttpStatus.BAD_REQUEST, "입력값을 다시 확인해 주세요.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return handleException(ex, request, response, HttpStatus.CONFLICT, "요청을 처리할 수 없는 상태입니다.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return handleException(ex, request, response, HttpStatus.valueOf(ex.getStatusCode().value()), resolveResponseStatusMessage(ex));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if ("GET".equalsIgnoreCase(request.getMethod()) && !expectsJson(request)) {
            logException(ex, request, HttpStatus.NOT_FOUND);
            return redirectWithAlert(request, response, FALLBACK_REDIRECT_PATH, NOT_FOUND_PAGE_MESSAGE);
        }
        return handleException(ex, request, response, HttpStatus.NOT_FOUND, NOT_FOUND_PAGE_MESSAGE);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Object handleValidationException(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return handleException(ex, request, response, HttpStatus.BAD_REQUEST, "입력값을 다시 확인해 주세요.");
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpectedException(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return handleException(ex, request, response, HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private Object handleException(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String userMessage
    ) {
        logException(ex, request, status);
        if (expectsJson(request)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "status", status.value(),
                            "message", userMessage
                    ));
        }

        String redirectUri = resolveRedirectUri(request);
        return redirectWithAlert(request, response, redirectUri, userMessage);
    }

    private void logException(Exception ex, HttpServletRequest request, HttpStatus status) {
        log.error(
                "Unhandled request failure: status={}, method={}, uri={}, query={}, referer={}, message={}",
                status.value(),
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                request.getHeader("Referer"),
                ex.getMessage(),
                ex
        );
    }

    private boolean expectsJson(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }

        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private String resolveRedirectUri(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return FALLBACK_REDIRECT_PATH;
        }

        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return FALLBACK_REDIRECT_PATH;
            }

            String query = uri.getQuery();
            return query == null || query.isBlank() ? path : path + "?" + query;
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to parse referer for redirect: referer={}", referer, ex);
            return FALLBACK_REDIRECT_PATH;
        }
    }

    private String redirectWithAlert(
            HttpServletRequest request,
            HttpServletResponse response,
            String redirectUri,
            String userMessage
    ) {
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put(ALERT_MESSAGE_ATTRIBUTE, userMessage);
        RequestContextUtils.saveOutputFlashMap(redirectUri, request, response);
        return "redirect:" + redirectUri;
    }

    private String resolveResponseStatusMessage(ResponseStatusException ex) {
        int statusCode = ex.getStatusCode().value();
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "요청한 정보를 찾을 수 없습니다.";
        }
        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            return "해당 요청에 접근할 수 없습니다.";
        }
        if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            return "로그인이 필요합니다.";
        }
        if (statusCode >= 400 && statusCode < 500) {
            return "요청을 다시 확인해 주세요.";
        }
        return "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    }
}
