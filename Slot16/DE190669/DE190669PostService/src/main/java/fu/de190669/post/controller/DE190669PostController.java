package fu.de190669.post.controller;

import fu.de190669.post.common.ApiException;
import fu.de190669.post.common.Create;
import fu.de190669.post.dto.ApiResponse;
import fu.de190669.post.dto.PostDTO;
import fu.de190669.post.entity.PostStatus;
import fu.de190669.post.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/posts", produces = "application/json")
public class DE190669PostController {
    private final PostService postService;

    public DE190669PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<ApiResponse> createPost(
            @Validated({Default.class, Create.class}) @RequestBody PostDTO dto) {
        return ResponseEntity.status(201).body(postService.createPost(dto));
    }

    @PutMapping(value = "/{postId}", consumes = "application/json")
    public ApiResponse updatePost(@PathVariable Long postId, @Valid @RequestBody PostDTO dto) {
        return postService.updatePost(postId, dto);
    }

    @GetMapping("/{postId}")
    public ApiResponse getPostById(@PathVariable Long postId) {
        return postService.getPostById(postId);
    }

    @DeleteMapping("/{postId}")
    public ApiResponse cancelPost(@PathVariable Long postId) {
        return postService.cancelPost(postId);
    }

    @GetMapping
    public ApiResponse getPostList(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String advertiserName,
                                   @RequestParam(required = false) PostStatus status) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(406, 2, "page must be >= 0 and size must be between 1 and 100");
        }
        return postService.getPostList(advertiserName, status, PageRequest.of(page, size, Sort.by("postId")));
    }
}
