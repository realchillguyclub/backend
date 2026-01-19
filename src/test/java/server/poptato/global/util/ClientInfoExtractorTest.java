package server.poptato.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import jakarta.servlet.http.HttpServletRequest;
import server.poptato.configuration.ServiceTestConfig;

class ClientInfoExtractorTest extends ServiceTestConfig {

    @Mock
    HttpServletRequest request;

    @InjectMocks
    ClientInfoExtractor clientInfoExtractor;

    @Nested
    @DisplayName("[SCN-UTIL-CLIENT-001] extractClientIp 테스트")
    class ExtractClientIpTest {

        @Test
        @DisplayName("[TC-CLIENT-001] X-Forwarded-For 헤더가 있으면 해당 IP를 반환한다")
        void extractClientIp_withXForwardedFor_returnsFirstIp() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18");

            // when
            String ip = clientInfoExtractor.extractClientIp(request);

            // then
            assertThat(ip).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("[TC-CLIENT-002] X-Forwarded-For 헤더가 단일 IP면 해당 IP를 반환한다")
        void extractClientIp_withSingleXForwardedFor_returnsIp() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");

            // when
            String ip = clientInfoExtractor.extractClientIp(request);

            // then
            assertThat(ip).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("[TC-CLIENT-003] Proxy-Client-IP 헤더가 있으면 해당 IP를 반환한다")
        void extractClientIp_withProxyClientIp_returnsIp() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn("10.0.0.1");

            // when
            String ip = clientInfoExtractor.extractClientIp(request);

            // then
            assertThat(ip).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("[TC-CLIENT-004] WL-Proxy-Client-IP 헤더가 있으면 해당 IP를 반환한다")
        void extractClientIp_withWLProxyClientIp_returnsIp() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("172.16.0.1");

            // when
            String ip = clientInfoExtractor.extractClientIp(request);

            // then
            assertThat(ip).isEqualTo("172.16.0.1");
        }

        @Test
        @DisplayName("[TC-CLIENT-005] 프록시 헤더가 없으면 remoteAddr를 반환한다")
        void extractClientIp_noProxyHeaders_returnsRemoteAddr() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // when
            String ip = clientInfoExtractor.extractClientIp(request);

            // then
            assertThat(ip).isEqualTo("127.0.0.1");
        }

        @Test
        @DisplayName("[TC-CLIENT-006] 헤더 값이 unknown이면 다음 헤더를 확인한다")
        void extractClientIp_withUnknownValue_checksNextHeader() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getHeader("Proxy-Client-IP")).thenReturn("10.0.0.50");

            // when
            String ip = clientInfoExtractor.extractClientIp(request);

            // then
            assertThat(ip).isEqualTo("10.0.0.50");
        }

        @Test
        @DisplayName("[TC-CLIENT-007] 헤더 값이 빈 문자열이면 다음 헤더를 확인한다")
        void extractClientIp_withEmptyValue_checksNextHeader() {
            // given
            when(request.getHeader("X-Forwarded-For")).thenReturn("");
            when(request.getHeader("Proxy-Client-IP")).thenReturn("");
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("");
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn("192.168.10.1");

            // when
            String ip = clientInfoExtractor.extractClientIp(request);

            // then
            assertThat(ip).isEqualTo("192.168.10.1");
        }
    }

    @Nested
    @DisplayName("[SCN-UTIL-CLIENT-002] extractUserAgent 테스트")
    class ExtractUserAgentTest {

        @Test
        @DisplayName("[TC-CLIENT-008] User-Agent 헤더가 있으면 해당 값을 반환한다")
        void extractUserAgent_withHeader_returnsValue() {
            // given
            when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            // when
            String userAgent = clientInfoExtractor.extractUserAgent(request);

            // then
            assertThat(userAgent).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        }

        @Test
        @DisplayName("[TC-CLIENT-009] User-Agent 헤더가 없으면 null을 반환한다")
        void extractUserAgent_noHeader_returnsNull() {
            // given
            when(request.getHeader("User-Agent")).thenReturn(null);

            // when
            String userAgent = clientInfoExtractor.extractUserAgent(request);

            // then
            assertThat(userAgent).isNull();
        }

        @Test
        @DisplayName("[TC-CLIENT-010] User-Agent가 512자 초과면 512자로 truncate된다")
        void extractUserAgent_longValue_truncatesTo512() {
            // given
            String longUserAgent = "A".repeat(600);
            when(request.getHeader("User-Agent")).thenReturn(longUserAgent);

            // when
            String userAgent = clientInfoExtractor.extractUserAgent(request);

            // then
            assertThat(userAgent).hasSize(512);
            assertThat(userAgent).isEqualTo("A".repeat(512));
        }

        @Test
        @DisplayName("[TC-CLIENT-011] User-Agent가 정확히 512자면 그대로 반환한다")
        void extractUserAgent_exactly512_returnsAsIs() {
            // given
            String exact512 = "B".repeat(512);
            when(request.getHeader("User-Agent")).thenReturn(exact512);

            // when
            String userAgent = clientInfoExtractor.extractUserAgent(request);

            // then
            assertThat(userAgent).hasSize(512);
            assertThat(userAgent).isEqualTo(exact512);
        }

        @Test
        @DisplayName("[TC-CLIENT-012] User-Agent가 512자 미만이면 그대로 반환한다")
        void extractUserAgent_lessThan512_returnsAsIs() {
            // given
            String shortAgent = "TestAgent/1.0";
            when(request.getHeader("User-Agent")).thenReturn(shortAgent);

            // when
            String userAgent = clientInfoExtractor.extractUserAgent(request);

            // then
            assertThat(userAgent).isEqualTo(shortAgent);
        }
    }
}
