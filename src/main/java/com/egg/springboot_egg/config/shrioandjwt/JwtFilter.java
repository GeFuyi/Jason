package com.egg.springboot_egg.config.shrioandjwt;

import io.jsonwebtoken.Claims;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.filter.AccessControlFilter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class JwtFilter extends AccessControlFilter {

    //filter在HTTPRequest到达Servlet(SpringMVC-Controller)之前进行拦截
    private final SecurityManager securityManager; // 注入 SecurityManager

    public JwtFilter(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        return false; // 交给 onAccessDenied 处理
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

//        System.out.println("[JwtFilter] Intercepted " + httpRequest.getMethod() + " request");
//        printRawHttpRequest(httpRequest);

        // =================== CORS 头 ===================
//        httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
        httpResponse.setHeader("Access-Control-Allow-Origin", httpRequest.getHeader("Origin"));
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type");
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        String path = httpRequest.getServletPath();
        System.out.println("[JwtFilter] Request path: " + path);
        System.out.println("[JwtFilter] Method: " + httpRequest.getMethod());

        // ------------------ 放行接口 ------------------
        if ("/user/login".equals(path) || "/user/register".equals(path) || path.startsWith("/ws-chat")) {
            return true;
        }

        // ------------------ 获取 Token ------------------
        String header = httpRequest.getHeader("Authorization");
        System.out.println("[JwtFilter] Authorization header: " + header);

        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("[JwtFilter] No Authorization header found");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("缺少Token");
            return false;
        }

        String token = header.substring(7);
        System.out.println("[JwtFilter] Extracted token: " + token);

        try {
            // 绑定 SecurityManager
            ThreadContext.bind(securityManager);
            System.out.println("[JwtFilter] SecurityManager bound to ThreadContext");

            // 解析 JWT
            Claims claims = JwtUtil.parseToken(token);
            String username = claims.getSubject();
            List<String> roles = (List<String>) claims.get("roles");
            List<String> permissions = (List<String>) claims.get("permissions");

            System.out.println("[JwtFilter] Token valid, username: " + username);
            System.out.println("[JwtFilter] roles: " + roles + ", permissions: " + permissions);

            // ------------------ 认证 Subject ------------------
            Subject subject = SecurityUtils.getSubject();
            if (!subject.isAuthenticated()) {
                JwtToken jwtToken = new JwtToken(username, token, roles, permissions);
                subject.login(jwtToken);
                System.out.println("[JwtFilter] Subject logged in successfully");
                ThreadContext.bind(subject);
                System.out.println("[JwtFilter] Subject bound to ThreadContext");
            }

            // 调试打印：确认认证状态
            System.out.println("[JwtFilter] After login, isAuthenticated: " + subject.isAuthenticated());
            System.out.println("[JwtFilter] After login, principal: " + subject.getPrincipal());

            return true; // 认证成功
        } catch (Exception e) {
            System.out.println("[JwtFilter] Token invalid or authentication failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private void printRawHttpRequest(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();

            // 请求行：GET /path HTTP/1.1
            sb.append(request.getMethod())
                    .append(" ")
                    .append(request.getRequestURI())
                    .append(request.getQueryString() != null ? "?" + request.getQueryString() : "")
                    .append(" ")
                    .append(request.getProtocol())
                    .append("\n");

            // 请求头
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String value = request.getHeader(name);
                sb.append(name).append(": ").append(value).append("\n");
            }

            // 请求体（只在 POST/PUT 等场景需要，小心不要影响流读取）
            // 如果需要 body，可以考虑用 HttpServletRequestWrapper 包装，或者在过滤器链最前面缓存 InputStream

            System.out.println("=========== RAW HTTP REQUEST ===========");
            System.out.println(sb.toString());
            System.out.println("========================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}