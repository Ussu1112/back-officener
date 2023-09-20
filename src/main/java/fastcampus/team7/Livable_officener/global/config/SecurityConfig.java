package fastcampus.team7.Livable_officener.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                    .antMatchers("/h2-console/**", "/api/building", "/api/auth", "/api/verify", "/api/confirm", "/api/signup", "/api/login", "/api/elevator","/api/chat/{roomid}/kickRequest")
                    .permitAll()
                    .anyRequest().authenticated()
                    .and()
                .formLogin()
                    .disable()
                .csrf()
                    .disable()
                .httpBasic()
                    .disable()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
