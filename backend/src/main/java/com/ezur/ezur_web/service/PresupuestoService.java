package com.ezur.ezur_web.service;

import com.ezur.ezur_web.dto.PresupuestoRequest;
import com.ezur.ezur_web.exception.RecursoNoEncontradoException;
import  com.ezur.ezur_web.model.EstadoPresupuesto;
import  com.ezur.ezur_web.model.Presupuesto;
import  com.ezur.ezur_web.repository.PresupuestoRepository;
import  org.springframework.stereotype.Service;


import  java.util.List;

@Service 

public class PresupuestoService {
    
    private final PresupuestoRepository repository;

    public PresupuestoService(PresupuestoRepository repository){
        this.repository=repository;
    }

    public Presupuesto guardar(Presupuesto presupuesto){
        return repository.save(presupuesto);
    }

    public  List<Presupuesto> listaTodos(){
        return repository.findAll();
    }

    public  Presupuesto buscarPorId(Long id){
       return repository.findById(id) .orElseThrow(() -> new RecursoNoEncontradoException("Presupuesto no encontrado con id: " + id));
    }

    public Presupuesto actualizarEstado(Long id, EstadoPresupuesto nuevoEstado){
        Presupuesto presupuesto = buscarPorId(id);
        presupuesto.setEstado(nuevoEstado);
        return repository.save(presupuesto);
    }

    public void eliminar(Long id){
        if (!repository.existsById(id)) {
            throw new RecursoNoEncontradoException("Presupuesto no encontrado con id: " + id);
        }
    repository.deleteById(id);
    }

    public long contarPorEstado(EstadoPresupuesto estado){
        return repository.countByEstado(estado);
    }

    public long contarTodos(){
        return  repository.count();
    }

    public List<Presupuesto> listarDelCliente(Long usuarioId){
        return repository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    // Un cliente solo puede cancelar solicitudes suyas que Ezur aún no ha contactado
    public boolean cancelarDelCliente(Long id, Long usuarioId){
        Presupuesto presupuesto = buscarPorId(id);
        if (!usuarioId.equals(presupuesto.getUsuarioId())
                || presupuesto.getEstado() != EstadoPresupuesto.PENDIENTE) {
            return false;
        }
        presupuesto.setEstado(EstadoPresupuesto.CANCELADO);
        repository.save(presupuesto);
        return true;
    }

    public Presupuesto guardarDesdeRequest(PresupuestoRequest request){
        return guardarDesdeRequest(request, null);
    }

    public Presupuesto guardarDesdeRequest(PresupuestoRequest request, Long usuarioId){
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setNombreCompleto(request.getNombreCompleto());
        presupuesto.setEmail(request.getEmail());
        presupuesto.setTelefono(request.getTelefono());
        presupuesto.setDireccionOrigen(request.getDireccionOrigen());
        presupuesto.setDireccionDestino(request.getDireccionDestino());
        presupuesto.setTipoMudanza(request.getTipoMudanza());
        presupuesto.setFechaEstimada(request.getFechaEstimada());
        presupuesto.setComentarios(request.getComentarios());
        presupuesto.setAceptaPrivacidad(request.getAceptaPrivacidad());
        presupuesto.setUsuarioId(usuarioId);
        return repository.save(presupuesto);
    }


}
