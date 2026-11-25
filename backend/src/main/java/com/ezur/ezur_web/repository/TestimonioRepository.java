package com.ezur.ezur_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ezur.ezur_web.model.Testimonio;


@Repository 
public interface TestimonioRepository extends JpaRepository<Testimonio, Long> {

    
    
}
