package mx.edu.cetys.software_quality_lab.users;

import mx.edu.cetys.software_quality_lab.pets.Pet;
import mx.edu.cetys.software_quality_lab.users.exceptions.DuplicateUsernameException;
import mx.edu.cetys.software_quality_lab.users.exceptions.InvalidUserDataException;
import mx.edu.cetys.software_quality_lab.users.exceptions.UserNotFoundException;
import mx.edu.cetys.software_quality_lab.validators.EmailValidatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    UserRepository userRepository;

    // EmailValidatorService debe ser mockeado — en pruebas unitarias no probamos dependencias externas
    @Mock
    EmailValidatorService emailValidatorService;

    @InjectMocks
    UserService userService;

    // Helper para construir un Pet simulado como si ya estuviera guardado en BD (con ID asignado)
    private User buildMockSavedUser(Long id, String username, String firstName, String lastName, String phone, String email, int age, String status) {
        var user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setAge(age);
        user.setStatus(UserStatus.valueOf(status));
        return user;
    }

    // ─── Caso exitoso ─────────────────────────────────────────────────────────

    @Test
    void shouldRegisterUserSuccessfully() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15);
        var mockSavedUser = buildMockSavedUser(1L, "valid_user", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15, "ACTIVE");

        when(userRepository.save(any())).thenReturn(mockSavedUser);
        when(emailValidatorService.isValid(anyString())).thenReturn(true);

        //Act
        var response = userService.registerUser(request);

        //Assert
        verify(userRepository, times(1)).save(any()); // se llamó exactamente una vez
        assertEquals(1L, response.id());
        assertEquals("valid_user", response.username());
        assertEquals("Juan", response.firstName());
        assertEquals("Perez", response.lastName());
        assertEquals("6462943298", response.phone());
        assertEquals("jun4#gmal.com", response.email());
        assertEquals(15, response.age());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void shouldGetUserByIdSuccessfully() {
        //Arrange
         var mockUser = buildMockSavedUser(1L, "valid_user", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15, "ACTIVE");
         when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(mockUser));

         //Act
        var response = userService.getUserById(1L);

        //Assert
        assertEquals(1L, response.id());
        assertEquals("valid_user", response.username());
        assertEquals("Juan", response.firstName());
        assertEquals("Perez", response.lastName());
        assertEquals("6462943298", response.phone());
        assertEquals("jun4#gmal.com", response.email());
        assertEquals(15, response.age());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void shouldSuspendActiveUserSuccessfully() {
        //Arrange
        var mockUser = buildMockSavedUser(1L, "valid_user", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15, "ACTIVE");
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(mockUser));
        when(userRepository.save(any())).thenReturn(mockUser);

        //Act
        var response = userService.suspendUser(1L);

        //Assert
        assertEquals("SUSPENDED", response.status());
        verify(userRepository, times(1)).save(any());
    }

    // ─── Validaciones de Username ─────────────────────────────────────────────

    @Test
    void shouldThrowWhenUsernameTooShort() {
        //Arrange
        var request = new UserController.UserRequest("usr", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15);

        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUsernameTooLong() {
        //Arrange
        var request = new UserController.UserRequest("this_is_a_very_long_username", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15);

        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    @Test
    void shouldThrowWhenUsernameHasInvalidChars() {
        //Arrange
        var request = new UserController.UserRequest("User@Name", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15);

        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUsernameStartsWithUnderscore() {
        //Arrange
        var request = new UserController.UserRequest("_ nombrevalido", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    @Test
    void shouldThrowWhenUsernameEndsWithUnderscore() {
        //Arrange
        var request = new UserController.UserRequest("nombrevalido_", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    // ─── Validaciones de Nombre ───────────────────────────────────────────────

    @Test
    void shouldThrowWhenFirstNameTooShort() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "J", "Perez", "6462943298", "jun4#gmal.com", 15);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    @Test
    void shouldThrowWhenFirstNameContainsNumbers() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Ju4n", "Perez", "6462943298", "jun4#gmal.com", 15);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    @Test
    void shouldThrowWhenLastNameTooShort() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Juan", "P", "6462943298", "jun4#gmal.com", 15);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    @Test
    void shouldThrowWhenLastNameContainsNumbers() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Juan", "P3rez", "6462943298", "jun4#gmal.com", 15);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    // ─── Validaciones de Age ─────────────────────────────────────────────────

    @Test
    void shouldThrowWhenAgeIsExactlyTwelve() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Juan", "Perez", "6462943298", "jun4#gmal.com", 12);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    @Test
    void shouldThrowWhenAgeIsBelowTwelve() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Juan", "Perez", "6462943298", "jun4#gmal.com", 5);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    @Test
    void shouldThrowWhenAgeExceedsMaximum() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Juan", "Perez", "6462943298", "jun4#gmal.com", 121);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    // ─── Validaciones de Phone ───────────────────────────────────────────────

    @Test
    void shouldThrowWhenPhoneHasWrongLength() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Juan", "Perez", "646294329", "jun4#gmal.com", 15);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    @Test
    void shouldThrowWhenPhoneContainsLetters() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Juan", "Perez", "64629A3298", "jun4#gmal.com", 15);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    // ─── Validación de Email ──────────────────────────────────────────────────

    @Test
    void shouldThrowWhenEmailIsInvalid() {
        //Arrange
        var request = new UserController.UserRequest("valid_user", "Juan", "Perez", "6462943298", "invalid-email", 15);
        when(emailValidatorService.isValid(anyString())).thenReturn(false);
        //Act & Assert
        assertThrows(InvalidUserDataException.class, () -> userService.registerUser(request));
    }

    // ─── Unicidad de Username ─────────────────────────────────────────────────

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        //Arrange
        var request = new UserController.UserRequest("existing_user", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15);
        when(emailValidatorService.isValid(anyString())).thenReturn(true);
        when(userRepository.existsByUsername("existing_user")).thenReturn(true);
        //Act & Assert
        assertThrows(DuplicateUsernameException.class, () -> userService.registerUser(request));
    }

    // ─── Not found ───────────────────────────────────────────────────────────

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void shouldThrowWhenSuspendingAlreadySuspendedUser() {
        var mockUser = buildMockSavedUser(1L, "valid_user", "Juan", "Perez", "6462943298", "jun4#gmal.com", 15, "SUSPENDED");
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        assertThrows(InvalidUserDataException.class, () -> userService.suspendUser(1L));
    }
}
