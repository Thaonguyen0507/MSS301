package fu.de190669.user.service.impl;

import fu.de190669.user.common.ApiException;
import fu.de190669.user.dto.ApiResponse;
import fu.de190669.user.dto.PageDTO;
import fu.de190669.user.dto.UserDTO;
import fu.de190669.user.entity.Role;
import fu.de190669.user.entity.User;
import fu.de190669.user.entity.UserStatus;
import fu.de190669.user.repository.UserRepository;
import fu.de190669.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApiResponse createUser(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ApiException(226, 3, "Username is duplicated");
        }
        User user = new User();
        apply(dto, user);
        if (dto.getStatus() == null) user.setStatus(UserStatus.ACTIVE);
        return ApiResponse.ok("User created successfully", toDTO(userRepository.save(user)));
    }

    @Override
    @Transactional
    public ApiResponse updateUser(Long userId, UserDTO dto) {
        User user = findUser(userId);
        if (dto.getUsername() != null
                && userRepository.existsByUsernameAndUserIdNot(dto.getUsername(), userId)) {
            throw new ApiException(226, 3, "Username is duplicated");
        }
        apply(dto, user);
        return ApiResponse.ok("User updated successfully", toDTO(userRepository.save(user)));
    }

    @Override
    public ApiResponse getUserById(Long userId) {
        return ApiResponse.ok("Successful", toDTO(findUser(userId)));
    }

    @Override
    @Transactional
    public ApiResponse deactivateUser(Long userId) {
        User user = findUser(userId);
        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(user);
        return ApiResponse.ok("User is set to DEACTIVATED successfully", null);
    }

    @Override
    public ApiResponse getUserList(Role role, UserStatus status, Pageable pageable) {
        Page<User> page = userRepository.search(role, status, pageable);
        return ApiResponse.ok("Successful", new PageDTO(page, page.map(this::toDTO).getContent()));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, 4, "User is not found"));
    }

    private void apply(UserDTO dto, User user) {
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getRole() != null) user.setRole(Role.valueOf(dto.getRole()));
        if (dto.getRatePerPost() != null) user.setRatePerPost(dto.getRatePerPost());
        if (dto.getMaxDailyPosts() != null) user.setMaxDailyPosts(dto.getMaxDailyPosts());
        if (dto.getLevel() != null) user.setLevel(dto.getLevel());
        if (dto.getStatus() != null) user.setStatus(UserStatus.valueOf(dto.getStatus()));
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setRatePerPost(user.getRatePerPost());
        dto.setMaxDailyPosts(user.getMaxDailyPosts());
        dto.setLevel(user.getLevel());
        dto.setStatus(user.getStatus().name());
        return dto;
    }
}
