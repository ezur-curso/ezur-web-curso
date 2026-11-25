package com.ezur.ezur_front.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad del sitio: la sesión, el CSRF de los formularios y los accesos. Las credenciales
 * las valida la API; aquí solo se guarda quién entró (con su token) en la sesión del servidor.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Hay dos accesos independientes (clientes en /login, administradores en /admin-login)
    // que envían el mismo formulario; el campo "ambito" dice desde cuál se entró.
    private static final String AMBITO_ADMIN = "admin";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiAuthenticationProvider proveedor) throws Exception {
        http
            // Un gestor propio y sin "padre": el login pasa una sola vez por la API
            .authenticationManager(new ProviderManager(proveedor))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/servicios", "/nosotros", "/contacto", "/cotizar", "/politica-privacidad").permitAll()
                .requestMatchers("/css/**", "/img/**", "/js/**").permitAll()
                .requestMatchers("/login", "/admin-login", "/registro").permitAll()
                .requestMatchers("/olvide-password", "/restablecer-password").permitAll()
                .requestMatchers("/2fa").hasRole("ADMIN")
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/cliente/**").hasRole("CLIENTE")
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            // /admin/** pide el acceso de administración; el resto de zonas protegidas, el de clientes
            // (el orden importa: se usa la primera regla que coincide)
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/admin-login"),
                    PathPatternRequestMatcher.withDefaults().matcher("/admin/**"))
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    PathPatternRequestMatcher.withDefaults().matcher("/**")))
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(SecurityConfig::alIniciarSesion)
                .failureHandler(SecurityConfig::alFallarElLogin)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) ->
                    response.sendRedirect(request.getContextPath()
                        + (esAdmin(authentication) ? "/admin-login" : "/")))
                .permitAll()
            );

        http.addFilterAfter(new SegundoFactorFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private static String paginaDeAcceso(HttpServletRequest request) {
        return AMBITO_ADMIN.equals(request.getParameter("ambito")) ? "/admin-login" : "/login";
    }

    private static void alIniciarSesion(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // El administrador pasa por el 2FA hacia /admin; el cliente va a su área
        response.sendRedirect(request.getContextPath() + (esAdmin(authentication) ? "/2fa" : "/cliente"));
    }

    private static void alFallarElLogin(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String motivo = exception instanceof DisabledException ? "?inactivo"
                : exception instanceof AmbitoIncorrectoException ? "?perfil" : "?error";
        response.sendRedirect(request.getContextPath() + paginaDeAcceso(request) + motivo);
    }
}
