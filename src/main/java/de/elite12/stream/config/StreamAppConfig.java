package de.elite12.stream.config;

import de.elite12.stream.util.CustomJwtAuthenticationConverter;
import org.apache.catalina.filters.RemoteIpFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableScheduling
public class StreamAppConfig {

    @Configuration
    @EnableWebSocket
    @EnableWebSocketMessageBroker
    public static class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
        @Autowired
        private TaskScheduler messageBrokerTaskScheduler;

        @Override
        public void configureMessageBroker(MessageBrokerRegistry config) {
            config.enableSimpleBroker("/topic").setHeartbeatValue(new long[]{30000, 30000}).setTaskScheduler(this.messageBrokerTaskScheduler);
            config.setApplicationDestinationPrefixes("/app");
        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            registry.addEndpoint("/sock").setAllowedOriginPatterns("*").withSockJS().setSessionCookieNeeded(false);
        }
    }

    @Configuration
    public static class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
        @Override
        protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
            messages
                    //Allow non destination messages (e.g. connect, unsubscribe, ...)
                    .nullDestMatcher().permitAll()
                    //Allow subscriptionto racecontrol topic
                    .simpSubscribeDestMatchers("/topic/racecontrol").permitAll()
                    //Deny all other messages
                    .anyMessage().denyAll();
        }

        @Override
        protected boolean sameOriginDisabled() {
            return true;
        }
    }

    @Configuration
    @EnableWebSecurity
    public static class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Autowired
        private CustomJwtAuthenticationConverter jwtAuthenticationConverter;


        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .csrf().disable()
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwtAuthenticationConverter).and().and()
                .headers().disable()
                .authorizeRequests().requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("actuator");
        }

        @Bean
        public RemoteIpFilter remoteIpFilter() {
            return new RemoteIpFilter();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
            configuration.setAllowedHeaders(Arrays.asList("origin", "content-type", "accept", "authorization"));
            configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
            configuration.setAllowCredentials(true);
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }
}
