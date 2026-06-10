package mcp.server.foundation.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import mcp.server.foundation.security.request_binding.ReqsAuthBinding;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@Profile("dev")
@RequestMapping("/api/auth")
public final class DevAuthCtrl {

  private final DevAuthService authService;

  public DevAuthCtrl(DevAuthService authService) {
    this.authService = Objects.requireNonNull(authService, "authService");
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void login(
      @RequestBody LoginReqsBody requestBody,
      HttpServletRequest request) {

    ReqsAuthBinding principal = authService.authenticate(requestBody.email(), requestBody.password());
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        principal,
        null,
        List.of(new SimpleGrantedAuthority(principal.authority())));
    SecurityContext context = new SecurityContextImpl(authentication);
    HttpSession session = request.getSession(true);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    SecurityContextHolder.setContext(context);
  }

  @GetMapping("/me")
  public AuthUserResponse me(@AuthenticationPrincipal ReqsAuthBinding principal) {
    if (principal == null) {
      throw new BadCredentialsException("Authentication required");
    }
    return new AuthUserResponse(
        principal.principalId(),
        principal.tenantType(),
        principal.platformSystem());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
  }
}

record LoginReqsBody(String email, String password) {

  LoginReqsBody {
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
  }
}

record AuthUserResponse(String principalId, String tenantType, boolean isPlatformSystem) {
}
