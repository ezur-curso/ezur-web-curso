package com.ezur.ezur_web.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ezur.ezur_web.model.Usuario;


@Repository 

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByUsuario(String usuario);

    boolean existsByDni(String dni);

    boolean existsByUsuario(String usuario);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByUsuarioAndIdNot(String usuario, Long id);
}
