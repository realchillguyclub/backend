package server.poptato.global.config;

import java.io.InputStream;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import server.poptato.global.exception.CustomException;
import server.poptato.global.response.status.ErrorStatus;

@Configuration
public class SwaggerConfig {
	
	@Bean
	public OpenAPI customOpenAPI() {
		OpenAPI openAPI = new OpenAPI()
			.info(new Info()
                        .title("Illdan API Documentation")
                        .version("v1.2.0")
				.description("Spring REST Docs with Swagger UI.")
				.contact(new Contact()
					.name("Sangho Han")
					.url("https://github.com/bbbang105")
					.email("hchsa77@gmail.com"))
			).servers(List.of(
				new Server().url("http://localhost:8085").description("로컬 서버"),
				new Server().url("https://prev-illdan.store").description("테스트 서버"),
				new Server().url("https://prev-illdan-prod.store").description("배포 서버")
			));
		try {
			// ✅ Swagger 전용 ObjectMapper 사용
			ObjectMapper swaggerMapper = Json.mapper();
			ClassPathResource resource = new ClassPathResource("static/docs/open-api-3.0.1.json");
			try (InputStream inputStream = resource.getInputStream()) {
				// REST Docs에서 생성한 open-api JSON -> OpenAPI 객체로 변환
				OpenAPI restDocsOpenAPI = swaggerMapper.readValue(inputStream, OpenAPI.class);
				
				// REST Docs Paths 적용
				if (restDocsOpenAPI.getPaths() != null) {
					openAPI.setPaths(restDocsOpenAPI.getPaths());
				}
				
				// REST Docs Components + Security 병합
				Components components = restDocsOpenAPI.getComponents() != null
					? restDocsOpenAPI.getComponents()
					: new Components();
				
				SecurityScheme bearerAuth = new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT");
				
				components.addSecuritySchemes("bearerAuth", bearerAuth);
				
				openAPI.components(components)
					.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
			}
		} catch (Exception e) {
			throw new CustomException(ErrorStatus._FAILED_TRANSLATE_SWAGGER);
		}
		
		return openAPI;
	}
}

