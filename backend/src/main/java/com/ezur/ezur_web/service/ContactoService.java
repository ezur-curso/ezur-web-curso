package com.ezur.ezur_web.service;

import com.ezur.ezur_web.dto.ContactoRequest;
import com.ezur.ezur_web.exception.RecursoNoEncontradoException;
import com.ezur.ezur_web.model.Contacto;
import com.ezur.ezur_web.repository.ContactoRepository;

import java.util.List;

import org.springframework.stereotype.Service;

@Service 
public class ContactoService {
    private final ContactoRepository repository;

    public ContactoService(ContactoRepository repository){
        this.repository = repository;
    }

    public Contacto guardar(Contacto contacto){
        return repository.save(contacto);
    }

    public List<Contacto>listarTodos(){
        return repository.findAll();
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
        throw new RecursoNoEncontradoException("Mensaje no encontrado con id: " + id);
     }
        repository.deleteById(id);
    }

    public Contacto guardarDesdeRequest(ContactoRequest request) {
    Contacto contacto = new Contacto();
    contacto.setNombre(request.getNombre());
    contacto.setEmail(request.getEmail());
    contacto.setTelefono(request.getTelefono());
    contacto.setAsunto(request.getAsunto());
    contacto.setMensaje(request.getMensaje());
    contacto.setAceptaPrivacidad(request.getAceptaPrivacidad());
    return repository.save(contacto);
}
}
