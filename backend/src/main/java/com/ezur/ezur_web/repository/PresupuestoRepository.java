package com.ezur.ezur_web.repository;
 
import com.ezur.ezur_web.model.EstadoPresupuesto;
import  com.ezur.ezur_web.model.Presupuesto;
import  org.springframework.data.jpa.repository.JpaRepository;
import  org.springframework.stereotype.Repository;

import java.util.List;

@Repository 

public interface PresupuestoRepository extends JpaRepository <Presupuesto , Long>{
    long countByEstado(EstadoPresupuesto estado);

    List<Presupuesto> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
} 