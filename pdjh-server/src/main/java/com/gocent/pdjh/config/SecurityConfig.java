package com.gocent.pdjh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults()) // 复用我们在 CorsConfig.java 里的强防护约定
            .csrf(AbstractHttpConfigurer::disable) // 禁用CSRF，由于采用了无状态鉴权防不住，已在 CORS 中收紧 Origin
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 将核心易被击穿的暴露外网的 HIS 推单网关利用 HTTP Basic Auth 给上保险锁
                .requestMatchers("/Integration/**").authenticated()
                
                // 大前端(大屏和医生端暂未分离独立内网网关开发Token)及静态主页开放（防死锁）
                .anyRequest().permitAll()
            )
            .httpBasic(withDefaults());
            
        return http.build();
    }
}
