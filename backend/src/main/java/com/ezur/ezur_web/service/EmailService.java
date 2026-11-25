package com.ezur.ezur_web.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notificaciones.email.destino}")
    private String correoDestino;

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void enviarNotificacion(String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(correoDestino);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mailSender.send(mensaje);
    }

    public void enviarAviso(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mailSender.send(mensaje);
    }

    /** Correo con diseño al buzón de EZUR*/
    public void enviarNotificacionHtml(String asunto, String plantilla, Map<String, Object> datos) {
        enviarHtml(correoDestino, asunto, plantilla, datos);
    }

    /** Correo con diseño a cualquier destinatario. */
    public void enviarHtml(String destinatario, String asunto, String plantilla, Map<String, Object> datos) {
        Context contexto = new Context();
        contexto.setVariables(datos);
        contexto.setVariable("baseUrl", baseUrl);

        String html = templateEngine.process("email/" + plantilla, contexto);

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText("Abre este correo en un cliente que soporte HTML para verlo completo.", html);
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo preparar el correo", e);
        }
    }
}