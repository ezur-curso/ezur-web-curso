package com.ezur.ezur_web.service;

import com.ezur.ezur_web.dto.TestimonioRequest;
import com.ezur.ezur_web.exception.RecursoNoEncontradoException;
import com.ezur.ezur_web.model.Testimonio;
import com.ezur.ezur_web.repository.TestimonioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestimonioService {

    private final TestimonioRepository repository;

    public TestimonioService(TestimonioRepository repository) {
        this.repository = repository;
    }

    public List<Testimonio> listarTodos() {
        return repository.findAll();
    }

    public Testimonio guardar(Testimonio testimonio) {
        return repository.save(testimonio);
    }

    public Testimonio buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Testimonio no encontrado: " + id));
    }

    public Testimonio crear(TestimonioRequest request) {
        Testimonio testimonio = new Testimonio();
        copiar(request, testimonio);
        return repository.save(testimonio);
    }

    public Testimonio actualizar(Long id, TestimonioRequest request) {
        Testimonio testimonio = buscarPorId(id);
        copiar(request, testimonio);
        return repository.save(testimonio);
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNoEncontradoException("Testimonio no encontrado: " + id);
        }
        repository.deleteById(id);
    }

    public TestimonioRequest aRequest(Testimonio testimonio) {
        TestimonioRequest request = new TestimonioRequest();
        request.setNombre(testimonio.getNombre());
        request.setTexto(testimonio.getTexto());
        request.setServicio(testimonio.getServicio());
        try {
            request.setEstrellas(Integer.parseInt(testimonio.getEstrellas().trim()));
        } catch (Exception e) {
            request.setEstrellas(5);
        }
        return request;
    }

    private void copiar(TestimonioRequest request, Testimonio testimonio) {
        testimonio.setNombre(request.getNombre().trim());
        testimonio.setTexto(request.getTexto().trim());
        String servicio = request.getServicio();
        testimonio.setServicio(servicio == null || servicio.isBlank() ? null : servicio.trim());
        testimonio.setEstrellas(String.valueOf(request.getEstrellas()));
    }
}