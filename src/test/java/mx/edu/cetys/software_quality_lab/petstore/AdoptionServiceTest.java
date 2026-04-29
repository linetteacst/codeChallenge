package mx.edu.cetys.software_quality_lab.petstore;

import mx.edu.cetys.software_quality_lab.petstore.exceptions.AdoptionNotFoundException;
import mx.edu.cetys.software_quality_lab.petstore.exceptions.MaxAdoptionsReachedException;
import mx.edu.cetys.software_quality_lab.petstore.exceptions.PetAlreadyAdoptedException;
import mx.edu.cetys.software_quality_lab.petstore.exceptions.UserNotEligibleException;
import mx.edu.cetys.software_quality_lab.pets.Pet;
import mx.edu.cetys.software_quality_lab.pets.PetRepository;
import mx.edu.cetys.software_quality_lab.pets.exceptions.PetNotFoundException;
import mx.edu.cetys.software_quality_lab.users.User;
import mx.edu.cetys.software_quality_lab.users.UserRepository;
import mx.edu.cetys.software_quality_lab.users.UserStatus;
import mx.edu.cetys.software_quality_lab.users.exceptions.InvalidUserDataException;
import mx.edu.cetys.software_quality_lab.users.exceptions.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdoptionServiceTest {

    @Mock
    AdoptionRepository adoptionRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    PetRepository petRepository;

    @InjectMocks
    AdoptionService adoptionService;

    private User buildMockUser(Long id, int age, String status) {
        var user = new User();
        user.setId(id);
        user.setUsername("testuser");
        user.setFirstName("Diego");
        user.setLastName("Perez");
        user.setPhone("6461234567");
        user.setEmail("digo@test.com");
        user.setAge(age);
        user.setStatus(UserStatus.valueOf(status));
        return user;
    }

    private Pet buildMockPet(Long id, boolean available) {
        var pet = new Pet("Firulais", "Perro", "Blanco", 3);
        pet.setId(id);
        pet.setAvailable(available);
        return pet;
    }

    private Adoption buildMockAdoption(Long id, User user, Pet pet, AdoptionStatus status) {
        var adoption = new Adoption(user, pet);
        adoption.setId(id);
        adoption.setStatus(status);
        return adoption;
    }

    // Caso exitoso — crear adopción
    @Test
    void shouldCreateAdoptionSuccessfully() {
        var user = buildMockUser(1L, 25, "ACTIVE");
        var pet = buildMockPet(2L, true);
        var adoptionRequest = new AdoptionController.AdoptionRequest(1L, 2L);
        var expectedAdoption = buildMockAdoption(10L, user, pet, AdoptionStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(petRepository.findById(2L)).thenReturn(Optional.of(pet));
        when(adoptionRepository.existsByPetIdAndStatus(2L, AdoptionStatus.ACTIVE)).thenReturn(false);
        when(adoptionRepository.countByUserIdAndStatus(1L, AdoptionStatus.ACTIVE)).thenReturn(0L);
        when(adoptionRepository.save(any())).thenReturn(expectedAdoption);

        var response = adoptionService.createAdoption(adoptionRequest);

        assertEquals(10L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(2L, response.petId());
        assertEquals("ACTIVE", response.status());
        
        verify(petRepository, times(1)).save(any());
        verify(adoptionRepository, times(1)).save(any());
        assertFalse(pet.isAvailable());
    }

    // Caso exitoso — cancelar adopción
    @Test
    void shouldCancelAdoptionSuccessfully() {
        var user = buildMockUser(1L, 25, "ACTIVE");
        var pet = buildMockPet(2L, false);
        var adoption = buildMockAdoption(10L, user, pet, AdoptionStatus.ACTIVE);
        
        when(adoptionRepository.findById(10L)).thenReturn(Optional.of(adoption));
        when(adoptionRepository.save(any())).thenReturn(adoption);

        var response = adoptionService.cancelAdoption(10L);

        assertEquals("CANCELLED", response.status());
        verify(adoptionRepository, times(1)).save(any());
        verify(petRepository, times(1)).save(any());
        assertTrue(pet.isAvailable());
    }

    // Elegibilidad del usuario — no existe
    @Test
    void shouldThrowWhenUserDoesNotExist() {
        var adoptionRequest = new AdoptionController.AdoptionRequest(1L, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adoptionService.createAdoption(adoptionRequest));
        verify(adoptionRepository, never()).save(any());
    }

    // Elegibilidad del usuario — suspendido
    @Test
    void shouldThrowWhenUserIsSuspended() {
        var user = buildMockUser(1L, 25, "SUSPENDED");
        var adoptionRequest = new AdoptionController.AdoptionRequest(1L, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(UserNotEligibleException.class, () -> adoptionService.createAdoption(adoptionRequest));
        verify(adoptionRepository, never()).save(any());
    }

    // Elegibilidad del usuario — menor de 18
    @Test
    void shouldThrowWhenUserIsTooYoungToAdopt() {
        var user = buildMockUser(1L, 17, "ACTIVE");
        var adoptionRequest = new AdoptionController.AdoptionRequest(1L, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(UserNotEligibleException.class, () -> adoptionService.createAdoption(adoptionRequest));
        verify(adoptionRepository, never()).save(any());
    }

    // Elegibilidad del usuario — caso límite (boundary): exactamente 17 años
    @Test
    void shouldThrowWhenUserIsExactlySeventeen() {
        var user = buildMockUser(1L, 17, "ACTIVE");
        var adoptionRequest = new AdoptionController.AdoptionRequest(1L, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(UserNotEligibleException.class, () -> adoptionService.createAdoption(adoptionRequest));
        verify(adoptionRepository, never()).save(any());
    }

    // Elegibilidad del pet — no existe
    @Test
    void shouldThrowWhenPetDoesNotExist() {
        var user = buildMockUser(1L, 25, "ACTIVE");
        var adoptionRequest = new AdoptionController.AdoptionRequest(1L, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(petRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(PetNotFoundException.class, () -> adoptionService.createAdoption(adoptionRequest));
        verify(adoptionRepository, never()).save(any());
    }

    // Elegibilidad del pet — ya tiene adopción activa
    @Test
    void shouldThrowWhenPetIsAlreadyAdopted() {
        var user = buildMockUser(1L, 25, "ACTIVE");
        var pet = buildMockPet(2L, false);
        var adoptionRequest = new AdoptionController.AdoptionRequest(1L, 2L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(petRepository.findById(2L)).thenReturn(Optional.of(pet));
        when(adoptionRepository.existsByPetIdAndStatus(2L, AdoptionStatus.ACTIVE)).thenReturn(true);

        assertThrows(PetAlreadyAdoptedException.class, () -> adoptionService.createAdoption(adoptionRequest));
        verify(adoptionRepository, never()).save(any());
    }

    // Límite de adopciones — usuario con 3 ya activas
    @Test
    void shouldThrowWhenUserHasReachedMaxAdoptions() {
        var user = buildMockUser(1L, 25, "ACTIVE");
        var pet = buildMockPet(2L, true);
        var adoptionRequest = new AdoptionController.AdoptionRequest(1L, 2L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(petRepository.findById(2L)).thenReturn(Optional.of(pet));
        when(adoptionRepository.existsByPetIdAndStatus(2L, AdoptionStatus.ACTIVE)).thenReturn(false);
        when(adoptionRepository.countByUserIdAndStatus(1L, AdoptionStatus.ACTIVE)).thenReturn(3L);

        assertThrows(MaxAdoptionsReachedException.class, () -> adoptionService.createAdoption(adoptionRequest));
        verify(adoptionRepository, never()).save(any());
    }

    // Límite de adopciones — usuario con 2 activas (debe poder adoptar)
    @Test
    void shouldAllowAdoptionWhenUserHasTwoActiveAdoptions() {
        var user = buildMockUser(1L, 25, "ACTIVE");
        var pet = buildMockPet(2L, true);
        var adoptionRequest = new AdoptionController.AdoptionRequest(1L, 2L);
        var expectedAdoption = buildMockAdoption(10L, user, pet, AdoptionStatus.ACTIVE);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(petRepository.findById(2L)).thenReturn(Optional.of(pet));
        when(adoptionRepository.existsByPetIdAndStatus(2L, AdoptionStatus.ACTIVE)).thenReturn(false);
        when(adoptionRepository.countByUserIdAndStatus(1L, AdoptionStatus.ACTIVE)).thenReturn(2L);
        when(adoptionRepository.save(any())).thenReturn(expectedAdoption);

        var response = adoptionService.createAdoption(adoptionRequest);

        assertNotNull(response);
        assertEquals(10L, response.id());
        verify(petRepository, times(1)).save(any());
        verify(adoptionRepository, times(1)).save(any());
    }

    // Cancelar adopción — no existe
    @Test
    void shouldThrowWhenCancellingNonExistentAdoption() {
        when(adoptionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(AdoptionNotFoundException.class, () -> adoptionService.cancelAdoption(10L));
        verify(adoptionRepository, never()).save(any());
    }

    // Cancelar adopción — ya cancelada
    @Test
    void shouldThrowWhenCancellingAlreadyCancelledAdoption() {
        var user = buildMockUser(1L, 25, "ACTIVE");
        var pet = buildMockPet(2L, false);
        var adoption = buildMockAdoption(10L, user, pet, AdoptionStatus.CANCELLED);
        
        when(adoptionRepository.findById(10L)).thenReturn(Optional.of(adoption));

        assertThrows(InvalidUserDataException.class, () -> adoptionService.cancelAdoption(10L));
        verify(adoptionRepository, never()).save(any());
    }
}
