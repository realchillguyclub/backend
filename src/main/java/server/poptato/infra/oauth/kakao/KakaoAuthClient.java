package server.poptato.infra.oauth.kakao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@ComponentScan
@FeignClient(name = "kakaoAuthClient", url = "https://kauth.kakao.com")
public interface KakaoAuthClient {

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    KakaoTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> form);
}
