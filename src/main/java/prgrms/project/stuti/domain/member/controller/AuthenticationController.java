package prgrms.project.stuti.domain.member.controller;

import static org.springframework.http.HttpHeaders.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import prgrms.project.stuti.domain.member.model.MemberRole;
import prgrms.project.stuti.domain.member.service.AuthenticationService;
import prgrms.project.stuti.domain.member.service.dto.MemberIdResponse;
import prgrms.project.stuti.domain.member.controller.dto.MemberSaveRequest;
import prgrms.project.stuti.global.token.TokenService;
import prgrms.project.stuti.global.token.TokenType;
import prgrms.project.stuti.global.token.Tokens;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthenticationController {

	private final TokenService tokenService;
	private final AuthenticationService authenticationService;

	@Value("${app.oauth.domain}")
	private String domain;
	// private String domain = "http://172.19.96.1:3000";

	@PostMapping("/signup")
	public ResponseEntity<MemberIdResponse> singup(HttpServletResponse response,
		@Valid @RequestBody MemberSaveRequest memberSaveRequest) {
		MemberIdResponse memberIdResponse = authenticationService.signupMember(
			MemberMapper.toMemberDto(memberSaveRequest));
		Long memberId = memberIdResponse.memberId();

		Tokens tokens = tokenService.generateTokens(memberId.toString(), MemberRole.ROLE_MEMBER.name());
		authenticationService.saveRefreshToken(memberId, tokens, tokenService.getRefreshPeriod());

		addAccessTokenToCookie(response, tokens.accessToken(), TokenType.JWT_TYPE);
		// ResponseCookie responseCookie = tokenService.addAccessTokenToCookie(tokens.accessToken(), TokenType.JWT_TYPE);
		// response.setHeader(SET_COOKIE, responseCookie.toString());

		URI uri = URI.create(domain);

		return ResponseEntity
			.created(uri)
			.body(memberIdResponse);
	}

	@GetMapping("/login/{memberId}")
	public ResponseEntity<Long> login (@PathVariable Long memberId, HttpServletResponse response) {
		Tokens tokens = tokenService.generateTokens(memberId.toString(), MemberRole.ROLE_MEMBER.name());
		authenticationService.saveRefreshToken(memberId, tokens, tokenService.getRefreshPeriod());

		ResponseCookie responseCookie = tokenService.addAccessTokenToCookie(tokens.accessToken(), TokenType.JWT_TYPE);
		response.addHeader(SET_COOKIE, responseCookie.toString());

		URI uri = URI.create(domain + "/");

		return ResponseEntity
			.created(uri)
			.body(memberId);
	}

	@PostMapping("/logout")
	public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String accessToken = tokenService.resolveToken(request);
		String accessTokenWithType = tokenService.tokenWithType(accessToken, TokenType.JWT_BLACKLIST);
		authenticationService.logout(accessToken, tokenService.getExpiration(accessToken), accessTokenWithType);

		response.sendRedirect(domain + "/");
	}

	private void addAccessTokenToCookie(HttpServletResponse response, String accessToken,
		TokenType tokenType) {
		Cookie cookie = new Cookie(AUTHORIZATION,
			URLEncoder.encode(tokenService.tokenWithType(accessToken, tokenType),
				StandardCharsets.UTF_8));
		cookie.setSecure(true);
		cookie.setHttpOnly(true);
		cookie.setMaxAge((int) tokenService.getAccessTokenPeriod());
		cookie.setPath("/");

		response.addCookie(cookie);
	}
}
