package com.salofresh.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "requestStartTime";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                              @NonNull Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                 @NonNull Object handler, Exception ex) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        long durationMs = startTime == null ? -1 : System.currentTimeMillis() - (long) startTime;
        log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
                response.getStatus(), durationMs);
    }
}
