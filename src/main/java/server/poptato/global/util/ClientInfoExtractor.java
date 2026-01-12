package server.poptato.global.util;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTP 요청에서 클라이언트 정보(IP, User-Agent)를 추출하는 유틸리티
 */
@Component
public class ClientInfoExtractor {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
    };

    private static final int MAX_USER_AGENT_LENGTH = 512;

    /**
     * 클라이언트 IP 주소를 추출한다.
     * 프록시/로드밸런서 환경을 고려하여 X-Forwarded-For 등의 헤더를 우선 확인한다.
     *
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소
     */
    public String extractClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * User-Agent를 추출한다.
     * DB 컬럼 크기를 고려하여 최대 512자로 truncate한다.
     *
     * @param request HTTP 요청 객체
     * @return User-Agent 문자열 (없으면 null)
     */
    public String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > MAX_USER_AGENT_LENGTH
                ? userAgent.substring(0, MAX_USER_AGENT_LENGTH)
                : userAgent;
    }
}
