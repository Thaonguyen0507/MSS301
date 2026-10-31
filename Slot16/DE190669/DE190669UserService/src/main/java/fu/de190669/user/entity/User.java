package fu.de190669.user.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;

@Entity
@Table(name = "users", schema = "dbo")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", columnDefinition = "int")
    private Long userId;
    @Nationalized
    @Column(name = "username", length = 30, nullable = false, unique = true)
    private String username;
    @Nationalized
    @Column(name = "email", length = 100, nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    @Nationalized
    @Column(name = "role", length = 15, nullable = false)
    private Role role;
    @Column(name = "rate_per_post", precision = 18, scale = 2, nullable = false)
    private BigDecimal ratePerPost;
    @Column(name = "max_daily_posts", nullable = false)
    private int maxDailyPosts;
    @Column(name = "level", nullable = false)
    private int level;
    @Enumerated(EnumType.STRING)
    @Nationalized
    @Column(name = "status", length = 15, nullable = false)
    private UserStatus status;


    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public BigDecimal getRatePerPost() { return ratePerPost; }
    public void setRatePerPost(BigDecimal ratePerPost) { this.ratePerPost = ratePerPost; }
    public int getMaxDailyPosts() { return maxDailyPosts; }
    public void setMaxDailyPosts(int maxDailyPosts) { this.maxDailyPosts = maxDailyPosts; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}
