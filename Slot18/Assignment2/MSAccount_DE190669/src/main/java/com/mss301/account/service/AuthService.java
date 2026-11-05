package com.mss301.account.service;

import com.mss301.account.dto.LoginRequest;
import com.mss301.account.dto.LoginResponse;
import com.mss301.account.entity.StoreAccount;
import com.mss301.account.repository.AccountRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        StoreAccount account = accountRepository.findByUsername(request.username())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        checkActive(account);
        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return tokensFor(account);
    }

    public LoginResponse refresh(String refreshToken) {
        try {
            var claims = jwtService.parseToken(refreshToken);
            if (!"refresh".equals(claims.get("type"))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is not a refresh token");
            }
            StoreAccount account = accountRepository.findById(Integer.parseInt(claims.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
            checkActive(account);
            return tokensFor(account);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
    }

    private void checkActive(StoreAccount account) {
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
        }
    }

    private LoginResponse tokensFor(StoreAccount account) {
        String subject = account.getAccountId().toString();
        return new LoginResponse(jwtService.generateAccessToken(subject, account.getRole()),
            jwtService.generateRefreshToken(subject), "Bearer", account.getRole(), account.getUsername());
    }
}
