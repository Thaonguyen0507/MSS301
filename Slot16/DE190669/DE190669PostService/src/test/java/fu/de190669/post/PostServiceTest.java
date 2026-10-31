package fu.de190669.post;

import feign.FeignException;
import feign.Request;
import feign.Response;
import fu.de190669.post.client.UserClient;
import fu.de190669.post.common.ApiException;
import fu.de190669.post.dto.*;
import fu.de190669.post.entity.Post;
import fu.de190669.post.entity.PostStatus;
import fu.de190669.post.repository.PostRepository;
import fu.de190669.post.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostServiceTest {
    private PostRepository repository;
    private UserClient client;
    private PostServiceImpl service;
    private UserDTO user;
    private Post post;

    @BeforeEach
    void setup() {
        repository = mock(PostRepository.class);
        client = mock(UserClient.class);
        service = new PostServiceImpl(repository, client, JsonMapper.builder().build());
        user = new UserDTO();
        user.setUserId(1L);
        user.setStatus("ACTIVE");
        user.setRatePerPost(new BigDecimal("500000.00"));
        user.setMaxDailyPosts(3);
        when(client.getUserById(1L)).thenReturn(ApiResponse.ok("Successful", user));
        when(repository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        post = new Post();
        post.setPostId(1L);
        post.setAdvertiserName("Shop ABC");
        post.setAdvertiserEmail("ads@example.com");
        post.setAdvertiserPhone("0900000000");
        post.setStartDate(java.sql.Date.valueOf("2026-07-25"));
        post.setEndDate(java.sql.Date.valueOf("2026-07-28"));
        post.setNumberOfPosts(2);
        post.setUserId(1L);
        post.setStatus(PostStatus.SCHEDULED);
        post.setTotalCost(new BigDecimal("1500000.00"));
        when(repository.findById(1L)).thenReturn(Optional.of(post));
    }

    private Date date(String value) {
        return Date.from(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private PostDTO request() {
        PostDTO dto = new PostDTO();
        dto.setAdvertiserName("Shop ABC");
        dto.setAdvertiserEmail("ads@example.com");
        dto.setAdvertiserPhone("0900000000");
        dto.setStartDate(date("2026-07-25"));
        dto.setEndDate(date("2026-07-28"));
        dto.setNumberOfPosts(2);
        dto.setUserId(1L);
        return dto;
    }

    @Test
    void createUsesRateTimesDaysAndIgnoresSubmittedCost() {
        PostDTO dto = request();
        dto.setTotalCost(BigDecimal.ONE);
        PostDTO result = (PostDTO) service.createPost(dto).getData();
        assertEquals(0, new BigDecimal("1500000").compareTo(result.getTotalCost()));
        assertEquals("SCHEDULED", result.getStatus());
    }

    @Test
    void feignNotFoundBecomes404Status4() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/users/99", Map.of(), null, StandardCharsets.UTF_8, null);
        FeignException missing = FeignException.errorStatus("getUserById", Response.builder()
                .status(404).reason("Not Found").request(request).headers(Map.of()).build());
        when(client.getUserById(99L)).thenThrow(missing);
        PostDTO dto = request();
        dto.setUserId(99L);
        ApiException ex = assertThrows(ApiException.class, () -> service.createPost(dto));
        assertEquals(404, ex.getHttpStatus());
        assertEquals(4, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void inactiveUserIsRejected() {
        user.setStatus("SUSPENDED");
        ApiException ex = assertThrows(ApiException.class, () -> service.createPost(request()));
        assertEquals(400, ex.getHttpStatus());
        assertEquals(5, ex.getStatus());
    }

    @Test
    void capacityIsCheckedOnCreate() {
        PostDTO dto = request();
        dto.setNumberOfPosts(4);
        assertEquals(406, assertThrows(ApiException.class, () -> service.createPost(dto)).getHttpStatus());
    }

    @Test
    void invalidPartialDatesDoNotChangeStoredPost() {
        PostDTO dto = new PostDTO();
        dto.setStartDate(date("2026-07-29"));
        assertEquals(406, assertThrows(ApiException.class, () -> service.updatePost(1L, dto)).getHttpStatus());
        assertEquals(java.sql.Date.valueOf("2026-07-25"), post.getStartDate());
        verify(repository, never()).save(any());
        verifyNoInteractions(client);
    }

    @Test
    void partialUpdateRecalculatesCurrentRateWithSqlDates() {
        user.setRatePerPost(new BigDecimal("600000.00"));
        PostDTO dto = new PostDTO();
        dto.setAdvertiserName("New name");
        PostDTO result = (PostDTO) service.updatePost(1L, dto).getData();
        assertEquals(0, new BigDecimal("1800000").compareTo(result.getTotalCost()));
        assertEquals("New name", result.getAdvertiserName());
        assertEquals(date("2026-07-25"), result.getStartDate());
        assertEquals(2, result.getNumberOfPosts());
    }

    @Test
    void partialUpdateValidatesCapacityBeforeMutation() {
        PostDTO dto = new PostDTO();
        dto.setNumberOfPosts(4);
        assertEquals(406, assertThrows(ApiException.class, () -> service.updatePost(1L, dto)).getHttpStatus());
        assertEquals(2, post.getNumberOfPosts());
        verify(repository, never()).save(any());
    }

    @Test
    void existingCampaignCanBeUpdatedWhenUserIsSuspended() {
        user.setStatus("SUSPENDED");
        PostDTO dto = new PostDTO();
        dto.setAdvertiserName("Updated campaign");
        assertEquals(1, service.updatePost(1L, dto).getStatus());
    }

    @Test
    void detailEmbedsUserAndCancellationKeepsRow() {
        PostDetailDTO detail = (PostDetailDTO) service.getPostById(1L).getData();
        assertEquals(1L, detail.getUser().getUserId());
        assertNull(service.cancelPost(1L).getData());
        assertEquals(PostStatus.CANCELLED, post.getStatus());
        verify(repository, never()).delete(any());
    }
}
