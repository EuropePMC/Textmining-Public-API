package uk.ac.ebi.literature.textminingapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class TextminingApiPublicWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

	private final MongoUserDetailsService userDetailsService;

    public TextminingApiPublicWebSecurityConfigurerAdapter(MongoUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configure(AuthenticationManagerBuilder builder) throws Exception {
        builder.userDetailsService(userDetailsService);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	http.csrf().disable().authorizeRequests()
        .antMatchers("/actuator/prometheus").permitAll()
        .antMatchers("/actuator/health").permitAll()
        .antMatchers("/result").permitAll()
        .anyRequest().authenticated()
        .and()
        .httpBasic()
        .and().sessionManagement().disable();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}