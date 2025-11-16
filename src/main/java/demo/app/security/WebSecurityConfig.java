package demo.app.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

  @Autowired(required = false)
  private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    if (oidcUserService != null) {
      http
          .authorizeHttpRequests(authz -> authz
              .requestMatchers("/user").authenticated()
              .requestMatchers("/**").permitAll()
          )
          .oauth2Login(oauth2 -> oauth2
              .userInfoEndpoint(userInfo -> userInfo
                  .oidcUserService(oidcUserService)
              )
          );
    } else {
      http
          .authorizeHttpRequests(authz -> authz
              .requestMatchers("/**").permitAll()
          );
    }
    return http.build();
  }
}