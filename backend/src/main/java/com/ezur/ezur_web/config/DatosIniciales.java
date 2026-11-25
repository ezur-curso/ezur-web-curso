package com.ezur.ezur_web.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ezur.ezur_web.model.TipoServicio;
import com.ezur.ezur_web.repository.TipoServicioRepository;

/** Carga las opciones de las listas desplegables la primera vez que la tabla está vacía. */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final TipoServicioRepository tipoServicioRepository;

    public DatosIniciales(TipoServicioRepository tipoServicioRepository) {
        this.tipoServicioRepository = tipoServicioRepository;
    }

    @Override
    public void run(String... args) {
        if (tipoServicioRepository.count() > 0) {
            return;
        }
        tipoServicioRepository.saveAll(List.of(
                new TipoServicio("Mudanza Local"),
                new TipoServicio("Mudanza Internacional"),
                new TipoServicio("Oficina / Comercio")));
    }
}
