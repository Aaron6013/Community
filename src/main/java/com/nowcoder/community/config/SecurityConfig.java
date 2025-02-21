package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author wensheng
 * @create 2022-03-08 10:36 上午
 **/
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                ).hasAnyAuthority(
                        AUTHORITY_USER,AUTHORITY_ADMIN,AUTHORITY_MODERATOR
        )
                .antMatchers("/discuss/top",
                        "/discuss/wonderful"
                        ).hasAnyAuthority(
                                AUTHORITY_MODERATOR)
                .antMatchers("/discuss/delete",
                        "/data/**")
                .hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll().and().csrf().disable();

        //权限不够时
        http.exceptionHandling().authenticationEntryPoint(new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                //没登陆，需要认证
                String header = request.getHeader("x-requested-with");
                if("XMLHttpRequest".equals(header)){
                    //异步请求
                    response.setContentType("application/plain;charset=utf-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(CommunityUtil.getJSONString(403,"你还没有登陆"));
                }else {
                    response.sendRedirect(request.getContextPath() + "/login");
                }
            }
        }).accessDeniedHandler(new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                //权限不够处理
                String header = request.getHeader("x-requested-with");
                if("XMLHttpRequest".equals(header)){
                    //异步请求
                    response.setContentType("application/plain;charset=utf-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(CommunityUtil.getJSONString(403,"你没有访问此功能的权限"));
                }else {
                    response.sendRedirect(request.getContextPath() + "/denied");
                }
            }
        });

        //security底层会拦截/logout请求，使得自己写的无法使用（security默认为filter）
        http.logout().logoutUrl("/securityLogout");
    }
}
