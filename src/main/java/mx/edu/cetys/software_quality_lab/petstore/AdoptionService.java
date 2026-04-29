package mx.edu.cetys.software_quality_lab.petstore;

import mx.edu.cetys.software_quality_lab.pets.PetRepository;
import mx.edu.cetys.software_quality_lab.pets.exceptions.PetNotFoundException;
import mx.edu.cetys.software_quality_lab.petstore.exceptions.AdoptionNotFoundException;
import mx.edu.cetys.software_quality_lab.petstore.exceptions.MaxAdoptionsReachedException;
import mx.edu.cetys.software_quality_lab.petstore.exceptions.PetAlreadyAdoptedException;
import mx.edu.cetys.software_quality_lab.petstore.exceptions.UserNotEligibleException;
import mx.edu.cetys.software_quality_lab.users.UserRepository;
import mx.edu.cetys.software_quality_lab.users.UserStatus;
import mx.edu.cetys.software_quality_lab.users.exceptions.InvalidUserDataException;
import mx.edu.cetys.software_quality_lab.users.exceptions.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdoptionService {

    private final Logger log = LoggerFactory.getLogger(AdoptionService.class);

    private final AdoptionRepository adoptionRepository;
    private final UserRepository userRepository;
    // petstore reutiliza el PetRepository del módulo pets/ — no duplica entidades
    private final PetRepository petRepository;

    public AdoptionService(AdoptionRepository adoptionRepository,
                           UserRepository userRepository,
                           PetRepository petRepository) {
        this.adoptionRepository = adoptionRepository;
        this.userRepository = userRepository;
        this.petRepository = petRepository;
    }

    /**
     * Listar todos los pets disponibles para adopción (available = true).
     */
    List<AdoptionController.AvailablePetResponse> listAvailablePets() {
        log.info("Obteniendo pets disponibles para adopción");
        // TODO: llamar a petRepository.findAllByAvailableTrue(), mapear cada Pet a AvailablePetResponse

        return petRepository.findAllByAvailableTrue()
                .stream()
                .map(pet -> new AdoptionController.AvailablePetResponse(
                        pet.getId(),
                        pet.getName(),
                        pet.getRace(),
                        pet.getColor(),
                        pet.getAge()
                        // ajusta los campos según tu record AvailablePetResponse
                ))
                .toList();
    }

    /**
     * Crear una adopción vinculando un usuario con un pet.
     *
     * Reglas a implementar (usar la excepción indicada en cada caso):
     *  1. El usuario debe existir                              → UserNotFoundException
     *  2. El status del usuario debe ser ACTIVE                → UserNotEligibleException
     *  3. La edad del usuario debe ser >= 18                   → UserNotEligibleException
     *  4. El pet debe existir                                  → PetNotFoundException
     *  5. El pet no debe tener una adopción ACTIVE             → PetAlreadyAdoptedException
     *  6. El usuario debe tener menos de 3 adopciones ACTIVE   → MaxAdoptionsReachedException
     *
     * En caso de éxito: marcar pet.available = false, guardar el pet, guardar la adopción y regresar respuesta.
     */
    AdoptionController.AdoptionResponse createAdoption(AdoptionController.AdoptionRequest request) {
        log.info("Creando adopción, userId={}, petId={}", request.userId(), request.petId());
        // TODO: implementar todas las reglas anteriores, persistir y mapear la respuesta
        //1. El usuario debe existir                              → UserNotFoundException
        var userDB = userRepository.findById(request.userId());
        if(userDB.isEmpty()) throw new UserNotFoundException("User Nor Found");

        // 2. El status del usuario debe ser ACTIVE                → UserNotEligibleException
        if(userDB.get().getStatus().equals(UserStatus.SUSPENDED)) throw new UserNotEligibleException("User no valido para adoptar");

        //3. La edad del usuario debe ser >= 18                   → UserNotEligibleException
        if(userDB.get().getAge()<18) throw new UserNotEligibleException("User no apto para adoptar");

        // 4. El pet debe existir                                  → PetNotFoundException
        var petDB = petRepository.findById(request.petId());
        if(petDB.isEmpty()) throw new PetNotFoundException("Pet Not Found");

        // 5. El pet no debe tener una adopción ACTIVE             → PetAlreadyAdoptedException
        if(adoptionRepository.existsByPetIdAndStatus(request.petId(),AdoptionStatus.ACTIVE)) throw new PetAlreadyAdoptedException("Pet adoptado anteriormente");

        //6. El usuario debe tener menos de 3 adopciones ACTIVE   → MaxAdoptionsReachedException
        var cant = adoptionRepository.countByUserIdAndStatus(request.userId(),AdoptionStatus.ACTIVE);
        if (cant>=3) throw new MaxAdoptionsReachedException("User ha alcanzado el limite de adopciones");

        petDB.get().setAvailable(false);
        petRepository.save(petDB.get());

        var savedAdoption = adoptionRepository.save(new Adoption(userDB.get(), petDB.get()));
        return mapToResponse(savedAdoption);
    }

    /**
     * Cancelar una adopción existente.
     *
     * Reglas a implementar:
     *  1. La adopción debe existir        → AdoptionNotFoundException
     *  2. La adopción debe estar ACTIVE   → InvalidUserDataException
     *
     * En caso de éxito: cambiar adoption.status = CANCELLED, marcar pet.available = true, guardar ambos.
     */
    AdoptionController.AdoptionResponse cancelAdoption(Long adoptionId) {
        log.info("Cancelando adopción, adoptionId={}", adoptionId);
        // TODO: implementar las reglas anteriores, persistir los cambios y mapear la respuesta
        var adoptionDB = adoptionRepository.findById(adoptionId);
        if(adoptionDB.isEmpty()) throw new AdoptionNotFoundException("Adoption Not Found");

        var adoption = adoptionDB.get();
        if (!adoption.getStatus().equals(AdoptionStatus.ACTIVE)) throw new InvalidUserDataException("Adopci[on no activa");

        adoption.setStatus(AdoptionStatus.CANCELLED);

        var pet = adoption.getPet();
        pet.setAvailable(true);

        var savedAdop = adoptionRepository.save(adoption);
        petRepository.save(pet);

        return mapToResponse(savedAdop);

    }

    private AdoptionController.AdoptionResponse mapToResponse(Adoption adoption) {
        // TODO: mapear los campos de la Entity Adoption al record AdoptionController.AdoptionResponse
        return new AdoptionController.AdoptionResponse(
                adoption.getId(),
                adoption.getUser().getId(),
                adoption.getPet().getId(),
                adoption.getStatus().toString(),
                adoption.getAdoptionDate().toString()
        );
    }
}
