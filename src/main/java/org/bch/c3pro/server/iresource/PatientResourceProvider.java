package org.bch.c3pro.server.iresource;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bch.c3pro.server.external.Queue;

import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;

/**
 * The Patient resource provider class
 * @author CHIP-IHL
 */
public class PatientResourceProvider extends C3PROResourceProvider implements IResourceProvider {



	/**
     * This map has a resource ID as a key, and each key maps to a Deque list containing all versions of the resource with that ID.
     */
    private Map<String, Deque<Patient>> myIdToPatientVersions = new HashMap<>();

    /**
     * This is used to generate new IDs
     */
    private long myNextId = 1;

    public PatientResourceProvider(Queue sqs) {
    		super(sqs);
    }

    @Override
    protected String generateNewId() {
        return UUID.randomUUID().toString();
    }

    @Override
    protected Class getResourceClass() {
        return Patient.class;
    }


    /**
     * The patient PUT handle
     * @param thePatient
     * @return
     */
    @Update()
    public MethodOutcome updatePatient(@ResourceParam Patient thePatient) {
        this.sendMessage(thePatient);
        if (thePatient.getAddress()!=null) {
            if (!thePatient.getAddress().isEmpty()) {
                // We get just the first adress
                AddressDt address = thePatient.getAddress().get(0);
                if (address.getState()!=null) {
//                    try {
//                        Utils.updateMapInfo(address.getState(), this.s3, 1);
//                    } catch (C3PROException e) {
//                        log.error(e.getMessage());
//                        e.printStackTrace();
//                    }
                }
            }
        }

        // Let the caller know the ID of the newly created resource
        return new MethodOutcome();
    }


    /**
     * Returns the resource type: Patient
     * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
     */
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }


}
