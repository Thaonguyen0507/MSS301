package fu.de190669.user;

import fu.de190669.user.common.Create;
import fu.de190669.user.dto.UserDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class UserValidationTest {
    @Test
    void partialUpdateSkipsOmittedFieldsButCreateRequiresThem() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            UserDTO dto = new UserDTO();
            dto.setEmail("new@example.com");
            assertTrue(validator.validate(dto).isEmpty());
            assertFalse(validator.validate(dto, Default.class, Create.class).isEmpty());
            dto.setEmail("invalid-email");
            assertFalse(validator.validate(dto).isEmpty());
        }
    }

    @Test
    void updateStillRejectsBlankNameInvalidEnumAndDecimalOverflow() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            UserDTO dto = new UserDTO();
            dto.setUsername("   ");
            assertFalse(validator.validate(dto).isEmpty());
            dto.setUsername("creator_a");
            dto.setRole("INVALID");
            assertFalse(validator.validate(dto).isEmpty());
            dto.setRole("CREATOR");
            dto.setRatePerPost(new BigDecimal("10000000000000000"));
            assertFalse(validator.validate(dto).isEmpty());
            dto.setRatePerPost(new BigDecimal("500000.00"));
            assertTrue(validator.validate(dto).isEmpty());
        }
    }
}
