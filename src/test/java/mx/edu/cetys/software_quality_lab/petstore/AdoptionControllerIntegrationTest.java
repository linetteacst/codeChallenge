package mx.edu.cetys.software_quality_lab.petstore;

import mx.edu.cetys.software_quality_lab.petstore.AdoptionRepository;
import mx.edu.cetys.software_quality_lab.pets.Pet;
import mx.edu.cetys.software_quality_lab.pets.PetRepository;
import mx.edu.cetys.software_quality_lab.users.User;
import mx.edu.cetys.software_quality_lab.users.UserRepository;
import mx.edu.cetys.software_quality_lab.users.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AdoptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdoptionRepository adoptionRepository;

    @Autowired
    private UserRepository userRepository;

    // petstore usa PetRepository del módulo pets/ directamente
    @Autowired
    private PetRepository petRepository;

    // Limpiar en orden correcto para respetar las relaciones de foreign key
    @BeforeEach
    public void limpiarBD() {
        adoptionRepository.deleteAll();
        petRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Helper: crear usuario elegible (ACTIVE, edad 25) directo en BD
    private User crearUsuarioElegible() {
        var user = new User("user4test", "Juan", "Perez", "6641234567", "juan4#gmail.com", 25);
        return userRepository.save(user);
    }

    // Helper: crear pet disponible para adopción directo en BD
    private Pet crearPetDisponible() {
        var pet = new Pet("Luna", "Labrador", "Negro", 3);
        pet.setAvailable(true);
        return petRepository.save(pet);
    }

    // GET /petstore/pets — listar pets disponibles
    @Test
    void shouldListAvailablePetsAndReturn200() throws Exception {
        crearPetDisponible();

        // TODO: realizar GET /petstore/pets
        // TODO: andExpect status 200
        // TODO: andExpect jsonPath("$.response").isArray() con al menos 1 elemento

        mockMvc.perform(
                        get("/petstore/pets")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response").isNotEmpty())
                .andExpect(jsonPath("$.error").isEmpty());
    }

    // POST /petstore/adoptions — crear adopción exitosamente
    @Test
    void shouldCreateAdoptionAndReturn201() throws Exception {
        var user = crearUsuarioElegible();
        var pet = crearPetDisponible();

        String body = """
                { "userId": %d, "petId": %d }""".formatted(user.getId(), pet.getId());

        // TODO: realizar POST /petstore/adoptions con body anterior
        // TODO: andExpect status 201
        // TODO: andExpect jsonPath("$.response.adoption.status") == "ACTIVE"

        mockMvc.perform(
                        post("/petstore/adoptions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.response.adoption.status").value("ACTIVE"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    // POST /petstore/adoptions — usuario suspendido
    @Test
    void shouldReturn422WhenUserIsSuspended() throws Exception {
        // TODO: guardar un usuario SUSPENDED y un pet disponible
        // TODO: realizar POST /petstore/adoptions
        // TODO: andExpect status 422

        var user = crearUsuarioElegible();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        var pet = crearPetDisponible();

        String body = """
                { "userId": %d, "petId": %d }
        """.formatted(user.getId(), pet.getId());

        mockMvc.perform(
                post("/petstore/adoptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // POST /petstore/adoptions — usuario menor de 18
    @Test
    void shouldReturn422WhenUserIsTooYoung() throws Exception {
        // TODO: guardar un usuario ACTIVE con edad 17 y un pet disponible
        // TODO: realizar POST /petstore/adoptions
        // TODO: andExpect status 422

        var user = crearUsuarioElegible();
        user.setStatus(UserStatus.ACTIVE);
        user.setAge(17);
        userRepository.save(user);

        var pet = crearPetDisponible();

        String body = """
                { "userId": %d, "petId": %d }
        """.formatted(user.getId(), pet.getId());

        mockMvc.perform(
                post("/petstore/adoptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // POST /petstore/adoptions — pet ya adoptado
    @Test
    void shouldReturn409WhenPetIsAlreadyAdopted() throws Exception {
        // TODO: guardar usuario elegible y pet disponible
        // TODO: crear primera adopción (via POST o repository)
        // TODO: realizar segundo POST /petstore/adoptions con el mismo pet
        // TODO: andExpect status 409

        var user1 = crearUsuarioElegible();
        var pet = crearPetDisponible();

        String body1 = """
                { "userId": %d, "petId": %d }""".formatted(user1.getId(), pet.getId());

        mockMvc.perform(post("/petstore/adoptions").contentType(MediaType.APPLICATION_JSON).content(body1))
                .andExpect(status().isCreated());

        var user2 = new User("user_dos", "Carlos", "Ruiz", "6641234567", "carlos#gmail.com", 30);
        userRepository.save(user2);

        String body2 = """
                { "userId": %d, "petId": %d }""".formatted(user2.getId(), pet.getId());

        mockMvc.perform(post("/petstore/adoptions").contentType(MediaType.APPLICATION_JSON).content(body2))
                .andExpect(status().isConflict()) // 409
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // POST /petstore/adoptions — usuario con 3 adopciones activas
    @Test
    void shouldReturn422WhenUserHasThreeActiveAdoptions() throws Exception {
        // TODO: guardar un usuario y cuatro pets disponibles
        // TODO: crear tres adopciones
        // TODO: realizar cuarta POST /petstore/adoptions
        // TODO: andExpect status 422

        var user = crearUsuarioElegible();

        for (int i = 0; i < 3; i++) {
            var pet = crearPetDisponible();
            String body = """
                    { "userId": %d, "petId": %d }""".formatted(user.getId(), pet.getId());
            mockMvc.perform(post("/petstore/adoptions").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }

        var pet4 = crearPetDisponible();
        String body4 = """
                { "userId": %d, "petId": %d }""".formatted(user.getId(), pet4.getId());

        mockMvc.perform(post("/petstore/adoptions").contentType(MediaType.APPLICATION_JSON).content(body4))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // POST /petstore/adoptions — usuario no existe
    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        var pet = crearPetDisponible();
        String body = """
                { "userId": 9999, "petId": %d }""".formatted(pet.getId());

        // TODO: realizar POST /petstore/adoptions
        // TODO: andExpect status 404

        mockMvc.perform(
                        post("/petstore/adoptions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // POST /petstore/adoptions — pet no existe
    @Test
    void shouldReturn404WhenPetDoesNotExist() throws Exception {
        var user = crearUsuarioElegible();
        String body = """
                { "userId": %d, "petId": 9999 }""".formatted(user.getId());

        // TODO: realizar POST /petstore/adoptions
        // TODO: andExpect status 404

        mockMvc.perform(
                        post("/petstore/adoptions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // PATCH /petstore/adoptions/{id}/cancel — cancelar exitosamente
    @Test
    void shouldCancelAdoptionAndReturn200() throws Exception {
        // TODO: guardar usuario, pet y crear una adopción ACTIVE via repository
        // TODO: realizar PATCH /petstore/adoptions/{id}/cancel
        // TODO: andExpect status 200
        // TODO: andExpect jsonPath("$.response.adoption.status") == "CANCELLED"

        var user = crearUsuarioElegible();
        var pet = crearPetDisponible();

        String createBody = """
                { "userId": %d, "petId": %d }""".formatted(user.getId(), pet.getId());

        mockMvc.perform(post("/petstore/adoptions").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated());

        Long adoptionId = adoptionRepository.findAll().get(0).getId();

        mockMvc.perform(
                        patch("/petstore/adoptions/" + adoptionId + "/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.adoption.status").value("CANCELLED"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    // PATCH /petstore/adoptions/{id}/cancel — adopción no existe
    @Test
    void shouldReturn404WhenAdoptionNotFound() throws Exception {
        // TODO: realizar PATCH /petstore/adoptions/9999/cancel
        // TODO: andExpect status 404

        mockMvc.perform(
                        patch("/petstore/adoptions/9999/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // PATCH /petstore/adoptions/{id}/cancel — ya cancelada
    @Test
    void shouldReturn400WhenAdoptionAlreadyCancelled() throws Exception {
        // TODO: guardar una adopción CANCELLED via repository
        // TODO: realizar PATCH /petstore/adoptions/{id}/cancel
        // TODO: andExpect status 400

        var user = crearUsuarioElegible();
        var pet = crearPetDisponible();

        String createBody = """
                { "userId": %d, "petId": %d }""".formatted(user.getId(), pet.getId());

        mockMvc.perform(post("/petstore/adoptions").contentType(MediaType.APPLICATION_JSON).content(createBody));
        Long adoptionId = adoptionRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/petstore/adoptions/" + adoptionId + "/cancel").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(
                        patch("/petstore/adoptions/" + adoptionId + "/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }
}
