package fu.de190669.user.service;

import fu.de190669.user.dto.ApiResponse;
import fu.de190669.user.dto.UserDTO;
import fu.de190669.user.entity.Role;
import fu.de190669.user.entity.UserStatus;
import org.springframework.data.domain.Pageable;

public interface UserService {
    ApiResponse createUser(UserDTO dto);
    ApiResponse updateUser(Long userId, UserDTO dto);
    ApiResponse getUserById(Long userId);
    ApiResponse deactivateUser(Long userId);
    ApiResponse getUserList(Role role, UserStatus status, Pageable pageable);
}
