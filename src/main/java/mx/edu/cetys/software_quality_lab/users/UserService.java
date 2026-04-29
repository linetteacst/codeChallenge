package mx.edu.cetys.software_quality_lab.users;

import mx.edu.cetys.software_quality_lab.users.exceptions.DuplicateUsernameException;
import mx.edu.cetys.software_quality_lab.users.exceptions.InvalidUserDataException;
import mx.edu.cetys.software_quality_lab.users.exceptions.UserNotFoundException;
import mx.edu.cetys.software_quality_lab.validators.EmailValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final EmailValidatorService emailValidatorService;


    public UserService(UserRepository userRepository, EmailValidatorService emailValidatorService) {
        this.userRepository = userRepository;
        this.emailValidatorService = emailValidatorService;
    }

    public static boolean isValidUsername(String user){
        if (user == null) return false;
        if(user.length()<5 || user.length() > 20) return false;
        if(user.charAt(0) == '_' || user.charAt(user.length()-1) == '_') return false;
        for (char c : user.toCharArray()){
            if (!Character.isLowerCase(c) && !Character.isDigit(c) && c != '_') return false;
        }
        return true;
    }

    public static boolean isValidName(String n){
        if (n == null) return false;
        if(n.length()<2 || n.length() > 50) return false;
        for (char c : n.toCharArray()) {
            if (!Character.isAlphabetic(c)) return false;
        }
        return true;
    }


    /**
     * Registrar un nuevo usuario aplicando todas las reglas de negocio.
     *
     * Reglas a implementar (lanzar InvalidUserDataException a menos que se indique):
     *  1. Username  — entre 5 y 20 caracteres, solo letras minúsculas, dígitos y guion bajo (_),
     *                 NO debe comenzar ni terminar con guion bajo
     *  2. First name — entre 2 y 50 caracteres, solo letras (se permiten acentos: á, é, ñ, etc.)
     *  3. Last name  — entre 2 y 50 caracteres, solo letras (se permiten acentos)
     *  4. Age        — debe ser mayor a 12 y menor o igual a 120
     *  5. Phone      — exactamente 10 dígitos, sin letras ni símbolos
     *  6. Email      — delegar a emailValidatorService.isValid(email);
     *                  lanzar InvalidUserDataException si regresa false
     *  7. Unicidad del username — si userRepository.existsByUsername regresa true,
     *                             lanzar DuplicateUsernameException
     */
    UserController.UserResponse registerUser(UserController.UserRequest request) {
        log.info("Iniciando registro de usuario, username={}", request.username());
        // TODO: implementar las reglas 1-7, luego guardar en BD y mapear la respuesta

        //  1. Username  — entre 5 y 20 caracteres, solo letras minúsculas, dígitos y guion bajo (_),
        //     *                 NO debe comenzar ni terminar con guion bajo

        if(!isValidUsername(request.username())) throw new InvalidUserDataException("Información no válida");

        //     *  2. First name — entre 2 y 50 caracteres, solo letras (se permiten acentos: á, é, ñ, etc.)
        if(!isValidName(request.firstName())) throw new InvalidUserDataException("Información no válida");

        // 3. Last name  — entre 2 y 50 caracteres, solo letras (se permiten acentos)
        if(!isValidName(request.lastName())) throw new InvalidUserDataException("Información no válida");

        // 4. Age        — debe ser mayor a 12 y menor o igual a 120
        if (request.age() <= 12 || request.age() > 120)throw new InvalidUserDataException("Información no válida");

        //5. Phone      — exactamente 10 dígitos, sin letras ni símbolos
        if(request.phone().length()!=10) throw new InvalidUserDataException("Información no válida");
        for(char c : request.phone().toCharArray()){
            if (!Character.isDigit(c)){
                throw new InvalidUserDataException("Información no válida");
            }
        }

        // 6. Email      — delegar a emailValidatorService.isValid(email);
        //     *                  lanzar InvalidUserDataException si regresa false
        if(!emailValidatorService.isValid(request.email())){
            throw  new InvalidUserDataException("El email ingresado no es válido");
        }

        //7. Unicidad del username — si userRepository.existsByUsername regresa true,
        //     *                             lanzar DuplicateUsernameException
        if(userRepository.existsByUsername(request.username())){
            throw  new DuplicateUsernameException("El user ingresado ya existe");
        }

        var savedUser = userRepository.save(
                new User(request.username(), request.firstName(), request.lastName(), request.phone(),request.email(),request.age())
        );

        log.info("User guardado exitosamente, id = {}", savedUser.getId());

        return mapToResponse(savedUser);
//        throw new UnsupportedOperationException("TODO: implementar registerUser");
    }

    /**
     * Buscar un usuario por ID.
     * Lanzar UserNotFoundException (HTTP 404) si el usuario no existe.
     */
    UserController.UserResponse getUserById(Long id) {
        log.info("Buscando usuario por ID, id={}", id);
        // TODO: buscar por id con findById, lanzar UserNotFoundException si está vacío, mapear y regresar
        if (id == null || id <= 0 ) throw new InvalidUserDataException("El ID del pet debe ser un número positivo");

        var userDB = userRepository.findById(id);

        if (userDB.isEmpty()){
            throw  new UserNotFoundException("User con id " + id + " no encontrado");
        }

        return mapToResponse(userDB.get());
    }

    /**
     * Suspender un usuario ACTIVO.
     * Lanzar UserNotFoundException si el usuario no existe.
     * Lanzar InvalidUserDataException si el usuario ya está SUSPENDED.
     */
    UserController.UserResponse suspendUser(Long id) {
        log.info("Suspendiendo usuario, id={}", id);
        // TODO: buscar usuario, validar status, cambiar a SUSPENDED, guardar, mapear y regresar
        if (id == null || id <= 0 ) throw new InvalidUserDataException("El ID del pet debe ser un número positivo");

        var userDB = userRepository.findById(id);

        if (userDB.isEmpty()){
            throw  new UserNotFoundException("User con id " + id + " no encontrado");
        }

        if(userDB.get().getStatus().equals(UserStatus.SUSPENDED)) throw new InvalidUserDataException("El usuario ya se encuentra suspendido");

        userDB.get().setStatus(UserStatus.SUSPENDED);
        userRepository.save(userDB.get());

        return mapToResponse(userDB.get());
    }

    private UserController.UserResponse mapToResponse(User user) {
        // TODO: mapear los campos de la Entity User al record UserController.UserResponse
        return new UserController.UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail(),
                user.getAge(),
                user.getStatus().toString()
        );
    }
}
