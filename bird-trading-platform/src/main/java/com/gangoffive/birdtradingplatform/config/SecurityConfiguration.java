package com.gangoffive.birdtradingplatform.config;

import com.gangoffive.birdtradingplatform.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.gangoffive.birdtradingplatform.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.gangoffive.birdtradingplatform.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.gangoffive.birdtradingplatform.security.oauth2.RestAuthenticationEntryPoint;
import com.gangoffive.birdtradingplatform.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.gangoffive.birdtradingplatform.enums.Permission.*;
import static com.gangoffive.birdtradingplatform.enums.UserRole.*;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true
)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }
    private static final String[] WHITE_LIST_URLS = {
            "/api/v1/auth/**",
            "/",
            "/auth/**",
            "/oauth2/**",
            "/error",
            "/user/me",
            "/api/v1/users/register",
            "/api/v1/users/authenticate",
            "/api/v1/users/reset-password",
            "/api/v1/users/verify/register",
            "/api/v1/users/verify/reset-password",
            "/api/v1/products",
            "/api/v1/products/**",
            "/api/v1/birds",
            "/api/v1/birds/**",
            "/api/v1/accessories",
            "/api/v1/accessories/**",
            "/api/v1/foods",
            "/api/v1/foods/**",
            "/upload",
            "/api/v1/users/get-cookie",
//            "/favicon.ico",
//            "/**/*.png",
//            "/**/*.gif",
//            "/**/*.svg",
//            "/**/*.jpg",
//            "/**/*.html",
//            "/**/*.css",
//            "/**/*.js",
            "/v2/api-docs",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new RestAuthenticationEntryPoint()))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(WHITE_LIST_URLS)
                                .permitAll()

                                .requestMatchers(HttpMethod.GET, "/api/v1/admin/**").hasAnyAuthority(ADMIN_READ.getPermission())
                                .requestMatchers(HttpMethod.POST, "/api/v1/admin/**").hasAnyAuthority(ADMIN_CREATE.getPermission())
                                .requestMatchers(HttpMethod.PUT, "/api/v1/admin/**").hasAnyAuthority(ADMIN_UPDATE.getPermission())
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/**").hasAnyAuthority(ADMIN_DELETE.getPermission())
                                .requestMatchers("/api/v1/admin/**").hasAnyRole(ADMIN.name())

                                .requestMatchers(HttpMethod.GET, "/api/v1/shopowner/**").hasAnyAuthority(SHOPOWNER_READ.getPermission())
                                .requestMatchers(HttpMethod.POST, "/api/v1/shopowner/**").hasAnyAuthority(SHOPOWNER_CREATE.getPermission())
                                .requestMatchers(HttpMethod.PUT, "/api/v1/shopowner/**").hasAnyAuthority(SHOPOWNER_UPDATE.getPermission())
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/shopowner/**").hasAnyAuthority(SHOPOWNER_DELETE.getPermission())
                                .requestMatchers("/api/v1/shopowner/**").hasAnyRole(SHOPOWNER.name())


                                .requestMatchers(HttpMethod.GET, "/api/v1/user/**").hasAnyAuthority(USER_READ.getPermission())
                                .requestMatchers(HttpMethod.POST, "/api/v1/user/**").hasAnyAuthority(USER_CREATE.getPermission())
                                .requestMatchers(HttpMethod.PUT, "/api/v1/user/**").hasAnyAuthority(USER_UPDATE.getPermission())
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/user/**").hasAnyAuthority(USER_DELETE.getPermission())
                                .requestMatchers("/api/v1/user/**").hasAnyRole(USER.name())


                                .requestMatchers(HttpMethod.GET, "/api/v1/shopstaff/**").hasAnyAuthority(SHOPSTAFF_READ.getPermission())
                                .requestMatchers("/api/v1/shopstaff/**").hasAnyRole(SHOPSTAFF.name())

                                .requestMatchers(HttpMethod.PUT, "/api/v1/users/**").hasAnyAuthority(USER_UPDATE.getPermission(), SHOPSTAFF_UPDATE.getPermission(), SHOPOWNER_UPDATE.getPermission())
                                .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyAuthority(USER_READ.getPermission(), SHOPSTAFF_READ.getPermission(), SHOPOWNER_READ.getPermission())
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasAnyAuthority(SHOPOWNER_DELETE.getPermission(), USER_DELETE.getPermission())
                                .requestMatchers("/api/v1/users/**").hasAnyRole(USER.name(), SHOPSTAFF.name(), SHOPOWNER.name())

                                .anyRequest()
                                .authenticated()
                )
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .oauth2Login(oauth2 -> oauth2.authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint.baseUri("/oauth2/authorize").authorizationRequestRepository(cookieAuthorizationRequestRepository()))
                        .redirectionEndpoint(redirectionEndpoint -> redirectionEndpoint.baseUri("/oauth2/callback/*"))
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        ;
        return http.build();
    }
}
