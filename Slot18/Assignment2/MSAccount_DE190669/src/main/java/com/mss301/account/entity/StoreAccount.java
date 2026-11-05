package com.mss301.account.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "StoreAccounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID")
    private Integer accountId;

    @Column(name = "Username", nullable = false, length = 20)
    private String username;

    @Column(name = "Email", nullable = false, length = 100)
    private String email;

    @Column(name = "Password", nullable = false, length = 100)
    private String password;

    @Column(name = "Role")
    private Integer role;

    @Builder.Default
    @Column(name = "IsActive", columnDefinition = "BIT DEFAULT 1")
    private Boolean isActive = true;
}
