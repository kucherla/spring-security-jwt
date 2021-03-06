package be.looorent.security.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

import static org.springframework.boot.autoconfigure.security.SecurityProperties.ACCESS_OVERRIDE_ORDER;
import static org.springframework.security.config.http.SessionCreationPolicy.NEVER;

/**
 * Configuration to register as a bean to enable JWT authentication.
 * @author Lorent Lempereur - lorent.lempereur.dev@gmail.com
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(ACCESS_OVERRIDE_ORDER)
@EnableConfigurationProperties({HttpHeaderProperties.class, AuthenticationProperties.class})
class JwtSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsFactory userDetailsFactory;

    @Autowired
    private HttpHeaderProperties httpHeaderProperties;

    @Autowired
    private AuthenticationProperties authenticationProperties;

    @Override
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Bean(name = "jwtAuthenticationManager")
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public AuthenticationCorsFilter corsFilter() {
        return new AuthenticationCorsFilter(httpHeaderProperties);
    }

    @Bean
    public AuthenticationProviderImpl jwtAuthenticationProvider() {
        return new AuthenticationProviderImpl(tokenParser(), userDetailsFactory);
    }

    @Bean
    @DependsOn
    public JwtTokenParser tokenParser() {
        return new JwtTokenParser(authenticationProperties);
    }

    @Bean
    public AuthenticationEntryPointImpl jwtEntryPoint() {
        return new AuthenticationEntryPointImpl();
    }

    @Bean
    public AuthenticationFilter jwtFilter(final AuthenticationEntryPointImpl entryPoint) throws Exception {
        return new AuthenticationFilter(authenticationManagerBean(), entryPoint);
    }

    @Bean
    public FilterRegistrationBean jwtAuthenticationFilterRegistration(final AuthenticationFilter filter) {
        final FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(filter);
        filterRegistrationBean.setEnabled(false);
        return filterRegistrationBean;
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(jwtAuthenticationProvider());
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring()
            .antMatchers(HttpMethod.OPTIONS, "/**")
            .antMatchers(authenticationProperties.getPublicRoute());
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.csrf().disable()
            .addFilterAfter(jwtFilter(jwtEntryPoint()), SecurityContextPersistenceFilter.class)
            .addFilterBefore(corsFilter(), AuthenticationFilter.class)
            .authorizeRequests().anyRequest().permitAll()
            .and()
            .sessionManagement().sessionCreationPolicy(NEVER);
    }
}
