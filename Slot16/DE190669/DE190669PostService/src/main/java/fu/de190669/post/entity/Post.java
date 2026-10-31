package fu.de190669.post.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "posts", schema = "dbo")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", columnDefinition = "int")
    private Long postId;
    @Nationalized
    @Column(name = "advertiser_name", nullable = false, length = 100)
    private String advertiserName;
    @Nationalized
    @Column(name = "advertiser_email", nullable = false, length = 100)
    private String advertiserEmail;
    @Nationalized
    @Column(name = "advertiser_phone", nullable = false, length = 20)
    private String advertiserPhone;
    @Temporal(TemporalType.DATE)
    @Column(name = "start_date", nullable = false)
    private Date startDate;
    @Temporal(TemporalType.DATE)
    @Column(name = "end_date", nullable = false)
    private Date endDate;
    @Column(name = "number_of_posts", nullable = false)
    private int numberOfPosts;
    @Column(name = "total_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost;
    @Nationalized
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private PostStatus status;
    @Column(name = "user_id", nullable = false, columnDefinition = "int")
    private Long userId;


    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getAdvertiserName() { return advertiserName; }
    public void setAdvertiserName(String advertiserName) { this.advertiserName = advertiserName; }
    public String getAdvertiserEmail() { return advertiserEmail; }
    public void setAdvertiserEmail(String advertiserEmail) { this.advertiserEmail = advertiserEmail; }
    public String getAdvertiserPhone() { return advertiserPhone; }
    public void setAdvertiserPhone(String advertiserPhone) { this.advertiserPhone = advertiserPhone; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public int getNumberOfPosts() { return numberOfPosts; }
    public void setNumberOfPosts(int numberOfPosts) { this.numberOfPosts = numberOfPosts; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public PostStatus getStatus() { return status; }
    public void setStatus(PostStatus status) { this.status = status; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
