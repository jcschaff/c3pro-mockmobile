package org.bch.c3pro.server.iresource;

import java.io.File;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.external.Queue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

/**
 * Abstract class for common resource provider methods
 * @author CHIP-IHL
 */
public abstract class C3PROResourceProvider {
	
    protected final Queue sqs;

    protected C3PROResourceProvider(Queue sqs) {
	    	this.sqs = sqs;
    }
    Log log = LogFactory.getLog(C3PROResourceProvider.class);

    protected FhirContext ctx = FhirContext.forDstu2();

    protected void sendMessage(BaseResource resource) throws InternalErrorException {
        String version="";
        IParser jsonParser = this.ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        String message = jsonParser.encodeResourceToString(resource);
        try {
            byte [] publicKeyBin = null;
            String publicKeyUUID = null;
            try {
                publicKeyBin = FileUtils.readFileToByteArray(new File(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY, AppConfig.SECURITY_PUBLICKEY_DEFAULT)));
                publicKeyUUID = FileUtils.readFileToString(new File(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY_ID, AppConfig.SECURITY_PUBLICKEY_ID_DEFAULT)));
                version = AppConfig.getProp(AppConfig.FHIR_VERSION_DEFAULT,AppConfig.FHIR_VERSION_DEFAULT_DEFAULT);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new InternalErrorException("Error reading public key or public key uuid from AWS S3", e);
            }
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBin);
            KeyFactory keyFactory = KeyFactory.getInstance(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY_BASEALG,AppConfig.SECURITY_PUBLICKEY_BASEALG_DEFAULT));
            PublicKey publicKey = keyFactory.generatePublic(publicSpec);
            sqs.sendMessageEncrypted(message, publicKey, publicKeyUUID, version);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalErrorException("Error sending message to Queue", e);
        }
    }

    protected abstract String generateNewId();
    protected abstract Class<BaseResource> getResourceClass();

}
