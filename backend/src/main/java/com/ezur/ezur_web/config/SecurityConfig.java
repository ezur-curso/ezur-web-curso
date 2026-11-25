package com.ezur.ezur_web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ezur.ezur_web.repository.UsuarioRepository;
import com.ezur.ezur_web.security.JwtAuthFilter;
import com.ezur.ezur_web.security.JwtService;

/** API sin estado: cada petición lleva su token, no hay sesión ni cookies (por eso no hace falta CSRF). */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt,
                                                   UsuarioRepository usuarios) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/registro", "/api/auth/olvide",
                        "/api/auth/restablecer", "/api/auth/restablecer/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/contacto", "/api/presupuestos").permitAll()
                .requestMatchers("/api/auth/2fa").hasRole("PRE_2FA")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/cliente/**").hasRole("CLIENTE")
                .anyRequest().denyAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                    responderJson(response, 401, "No autenticado"))
                .accessDeniedHandler((request, response, e) ->
                    responderJson(response, 403, "Sin permiso para esta operación")))
            .addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtAuthFilter(jwt, usuarios), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void responderJson(jakarta.servlet.http.HttpServletResponse response, int estado, String mensaje)
            throws java.io.IOException {
        response.setStatus(estado);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
