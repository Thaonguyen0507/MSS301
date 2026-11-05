package com.mss301.account;

import com.mss301.account.entity.StoreAccount;
import com.mss301.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (accountRepository.count() != 0) {
            return;
        }
        String password = passwordEncoder.encode("@1");
        accountRepository.saveAll(List.of(
            account("adminsys", "admin@drawingpen.com.vn", password, 1, true),
            account("staff02", "staff02@drawingpen.com.vn", password, 2, true),
            account("member01", "member01@drawingpen.com.vn", password, 3, true),
            account("member02", "member02@drawingpen.com.vn", password, 3, false),
            account("staff01", "staff01@drawingpen.com.vn", password, 2, false)
        ));
        log.info("Seeded 5 accounts");
    }

    private StoreAccount account(String username, String email, String password, int role, boolean active) {
        return StoreAccount.builder().username(username).email(email).password(password)
            .role(role).isActive(active).build();
    }
}
