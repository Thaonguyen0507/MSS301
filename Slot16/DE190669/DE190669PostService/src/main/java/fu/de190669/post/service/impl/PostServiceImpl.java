package fu.de190669.post.service.impl;

import feign.FeignException;
import fu.de190669.post.client.UserClient;
import fu.de190669.post.common.ApiException;
import fu.de190669.post.dto.*;
import fu.de190669.post.entity.Post;
import fu.de190669.post.entity.PostStatus;
import fu.de190669.post.repository.PostRepository;
import fu.de190669.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    public PostServiceImpl(PostRepository postRepository, UserClient userClient, ObjectMapper objectMapper) {
        this.postRepository = postRepository;
        this.userClient = userClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ApiResponse createPost(PostDTO dto) {
        validateDates(dto.getStartDate(), dto.getEndDate());
        UserDTO user = fetchUser(dto.getUserId());
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException(400, 5, "User is not ACTIVE for posting");
        }
        validateCapacity(user, dto.getNumberOfPosts());
        Post post = new Post();
        apply(dto, post);
        if (dto.getStatus() == null) post.setStatus(PostStatus.SCHEDULED);
        post.setTotalCost(computeTotal(user, post.getStartDate(), post.getEndDate()));
        return ApiResponse.ok("Post campaign created successfully", toDTO(postRepository.save(post)));
    }

    @Override
    @Transactional
    public ApiResponse updatePost(Long postId, PostDTO dto) {
        Post post = findPost(postId);
        Date start = dto.getStartDate() == null ? post.getStartDate() : dto.getStartDate();
        Date end = dto.getEndDate() == null ? post.getEndDate() : dto.getEndDate();
        validateDates(start, end);
        Long userId = dto.getUserId() == null ? post.getUserId() : dto.getUserId();
        int numberOfPosts = dto.getNumberOfPosts() == null ? post.getNumberOfPosts() : dto.getNumberOfPosts();
        UserDTO user = fetchUser(userId);
        validateCapacity(user, numberOfPosts);
        BigDecimal total = computeTotal(user, start, end);
        // All checks run before mutating the managed entity.
        apply(dto, post);
        post.setTotalCost(total);
        return ApiResponse.ok("Post campaign updated successfully", toDTO(postRepository.save(post)));
    }

    @Override
    public ApiResponse getPostById(Long postId) {
        return ApiResponse.ok("Successful", toDetailDTO(findPost(postId)));
    }

    @Override
    @Transactional
    public ApiResponse cancelPost(Long postId) {
        Post post = findPost(postId);
        post.setStatus(PostStatus.CANCELLED);
        postRepository.save(post);
        return ApiResponse.ok("Post campaign is cancelled successfully", null);
    }

    @Override
    public ApiResponse getPostList(String advertiserName, PostStatus status, Pageable pageable) {
        String name = advertiserName == null || advertiserName.isBlank() ? null : advertiserName;
        Page<Post> page = postRepository.search(name, status, pageable);
        return ApiResponse.ok("Successful", new PageDTO(page, page.map(this::toDetailDTO).getContent()));
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(404, 4, "Post is not found"));
    }

    private UserDTO fetchUser(Long userId) {
        try {
            ApiResponse response = userClient.getUserById(userId);
            if (response == null || response.getStatus() != 1 || response.getData() == null) {
                throw new ApiException(404, 4, "User ID is not found");
            }
            return objectMapper.convertValue(response.getData(), UserDTO.class);
        } catch (FeignException.NotFound ex) {
            throw new ApiException(404, 4, "User ID is not found");
        }
    }

    private void validateCapacity(UserDTO user, int numberOfPosts) {
        if (numberOfPosts < 1 || numberOfPosts > user.getMaxDailyPosts()) {
            throw new ApiException(406, 2, "numberOfPosts must be between 1 and user's maxDailyPosts");
        }
    }

    private void validateDates(Date start, Date end) {
        if (!toLocalDate(end).isAfter(toLocalDate(start))) {
            throw new ApiException(406, 2, "endDate must be after startDate");
        }
    }

    private BigDecimal computeTotal(UserDTO user, Date start, Date end) {
        long days = ChronoUnit.DAYS.between(toLocalDate(start), toLocalDate(end));
        BigDecimal total = user.getRatePerPost().multiply(BigDecimal.valueOf(days));
        if (total.precision() - total.scale() > 16) {
            throw new ApiException(406, 2, "totalCost exceeds decimal(18,2)");
        }
        return total;
    }

    private LocalDate toLocalDate(Date date) {
        // SQL DATE has no time; java.sql.Date.toInstant() is unsupported.
        if (date instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private Date toDate(Date date) {
        return Date.from(toLocalDate(date).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private void apply(PostDTO dto, Post post) {
        if (dto.getAdvertiserName() != null) post.setAdvertiserName(dto.getAdvertiserName());
        if (dto.getAdvertiserEmail() != null) post.setAdvertiserEmail(dto.getAdvertiserEmail());
        if (dto.getAdvertiserPhone() != null) post.setAdvertiserPhone(dto.getAdvertiserPhone());
        if (dto.getStartDate() != null) post.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) post.setEndDate(dto.getEndDate());
        if (dto.getNumberOfPosts() != null) post.setNumberOfPosts(dto.getNumberOfPosts());
        if (dto.getUserId() != null) post.setUserId(dto.getUserId());
        if (dto.getStatus() != null) post.setStatus(PostStatus.valueOf(dto.getStatus()));
    }

    private PostDTO toDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setPostId(post.getPostId());
        dto.setAdvertiserName(post.getAdvertiserName());
        dto.setAdvertiserEmail(post.getAdvertiserEmail());
        dto.setAdvertiserPhone(post.getAdvertiserPhone());
        dto.setStartDate(toDate(post.getStartDate()));
        dto.setEndDate(toDate(post.getEndDate()));
        dto.setNumberOfPosts(post.getNumberOfPosts());
        dto.setTotalCost(post.getTotalCost());
        dto.setStatus(post.getStatus().name());
        dto.setUserId(post.getUserId());
        return dto;
    }

    private PostDetailDTO toDetailDTO(Post post) {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setPostId(post.getPostId());
        dto.setAdvertiserName(post.getAdvertiserName());
        dto.setAdvertiserEmail(post.getAdvertiserEmail());
        dto.setAdvertiserPhone(post.getAdvertiserPhone());
        dto.setStartDate(toDate(post.getStartDate()));
        dto.setEndDate(toDate(post.getEndDate()));
        dto.setNumberOfPosts(post.getNumberOfPosts());
        dto.setTotalCost(post.getTotalCost());
        dto.setStatus(post.getStatus().name());
        dto.setUser(fetchUser(post.getUserId()));
        return dto;
    }
}
