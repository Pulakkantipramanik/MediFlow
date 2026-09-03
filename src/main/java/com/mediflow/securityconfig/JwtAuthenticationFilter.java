package com.mediflow.securityconfig;

import com.mediflow.user.entity.User;
import com.mediflow.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Read Authorization header
        String authHeader = request.getHeader("Authorization");

        // 2. If token is not present, continue request
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Remove "Bearer " from the header
        String token = authHeader.substring(7);

        try {

            // 4. Validate token
            if (!jwtService.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 5. Extract email from JWT
            String email = jwtService.extractEmail(token);

            // 6. Check if authentication is already available
            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // 7. Find user from database
                User user = userRepository.findByEmail(email)
                        .orElse(null);

                if (user != null) {

                    // 8. Get user's role
                    SimpleGrantedAuthority authority =
                            new SimpleGrantedAuthority(user.getRole());

                    // 9. Create Authentication object
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user.getEmail(),
                                    null,
                                    List.of(authority)
                            );

                    // 10. Store authentication in SecurityContext
                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception e) {

            // Invalid/expired/malformed JWT
            SecurityContextHolder.clearContext();
        }

        // 11. Continue request
        filterChain.doFilter(request, response);
    }
}