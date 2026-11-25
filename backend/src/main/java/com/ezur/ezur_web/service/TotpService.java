package com.ezur.ezur_web.service;

import org.springframework.stereotype.Service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service 
public class TotpService {
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public String generarSecreto(){
        return secretGenerator.generate();
    }

    public String generarQrDataUri(String secreto, String email){
        QrData data = new QrData.Builder()
                .label(email)              
                .secret(secreto)
                .issuer("EZUR Admin")     
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            byte[] imagen = qrGenerator.generate(data);
            return getDataUriForImage(imagen, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new IllegalStateException("No se pudo generar el código QR", e);
        }
    }

    public boolean codigoValido(String secreto, String codigo) {
        return codigo != null && codeVerifier.isValidCode(secreto, codigo.trim());
    }
}
