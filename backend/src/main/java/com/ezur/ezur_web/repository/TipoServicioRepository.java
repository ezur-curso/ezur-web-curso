package com.ezur.ezur_web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ezur.ezur_web.model.TipoServicio;

@Repository
public interface TipoServicioRepository extends JpaRepository<TipoServicio, Long> {

    List<TipoServicio> findAllByOrderByIdAsc();

    boolean existsByNombre(String nombre);
}
