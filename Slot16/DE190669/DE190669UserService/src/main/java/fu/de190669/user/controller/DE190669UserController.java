package fu.de190669.user.controller;

import fu.de190669.user.common.ApiException;
import fu.de190669.user.common.Create;
import fu.de190669.user.dto.ApiResponse;
import fu.de190669.user.dto.UserDTO;
import fu.de190669.user.entity.Role;
import fu.de190669.user.entity.UserStatus;
import fu.de190669.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/users", produces = "application/json")
public class DE190669UserController {
    private final UserService userService;

    public DE190669UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<ApiResponse> createUser(
            @Validated({Default.class, Create.class}) @RequestBody UserDTO dto) {
        return ResponseEntity.status(201).body(userService.createUser(dto));
    }

    @PutMapping(value = "/{userId}", consumes = "application/json")
    public ApiResponse updateUser(@PathVariable Long userId, @Valid @RequestBody UserDTO dto) {
        return userService.updateUser(userId, dto);
    }

    @GetMapping("/{userId}")
    public ApiResponse getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse deactivateUser(@PathVariable Long userId) {
        return userService.deactivateUser(userId);
    }

    @GetMapping
    public ApiResponse getUserList(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) Role role,
                                   @RequestParam(required = false) UserStatus status) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(406, 2, "page must be >= 0 and size must be between 1 and 100");
        }
        return userService.getUserList(role, status, PageRequest.of(page, size, Sort.by("userId")));
    }
}
