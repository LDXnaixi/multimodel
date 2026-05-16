package com.changan.multimodal.user.filter;

import com.changan.multimodal.user.service.UserAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccessAuditFilter extends OncePerRequestFilter {

    private final UserAuditService userAuditService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        userAuditService.recordAccess(
                request.getHeader("X-User-Name"),
                resolveIp(request),
                request.getRequestURI(),
                request.getMethod()
        );
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return ip == null || ip.isBlank() ? request.getRemoteAddr() : ip.split(",")[0].trim();
    }
}
