package org.bch.c3pro.server.iresource;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import org.bch.c3pro.server.external.Queue;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;

/**
 * The observation resource provider class
 * @author CHIP-IHL
*/
public class ObservationResourceProvider extends C3PROResourceProvider implements IResourceProvider {

	private Map<String, Deque<Observation>> myIdToQVersions = new HashMap<>();

	public ObservationResourceProvider(Queue sqs) {
		super(sqs);
	}

	@Override
    protected String generateNewId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the resource type: Observation
     * @return
     */
    @Override
    public Class<Observation> getResourceType() {
        return Observation.class;
    }

    @Override
    protected Class getResourceClass() {
        return Observation.class;
    }

    /**
     * Create Observation POST handle
     * @param theQA The observation
     * @return
     */
    @Create()
    public MethodOutcome createObservation(@ResourceParam Observation theQA) {
        String newId = generateNewId();
        addNewVersion(theQA, newId);
        this.sendMessage(theQA);
        // Let the caller know the ID of the newly created resource
        return new MethodOutcome(new IdDt(newId));
    }

    private void addNewVersion(Observation theQA, String theId) {
        InstantDt publishedDate;
        if (!myIdToQVersions.containsKey(theId)) {
            myIdToQVersions.put(theId, new LinkedList<Observation>());
            publishedDate = InstantDt.withCurrentTime();
        } else {
            Observation currentQA = myIdToQVersions.get(theId).getLast();
            Map<ResourceMetadataKeyEnum<?>, Object> resourceMetadata = currentQA.getResourceMetadata();
            publishedDate = (InstantDt) resourceMetadata.get(ResourceMetadataKeyEnum.PUBLISHED);
        }

		/*
		 * PUBLISHED time will always be set to the time that the first version was stored. UPDATED time is set to the time that the new version was stored.
		 */
        theQA.getResourceMetadata().put(ResourceMetadataKeyEnum.PUBLISHED, publishedDate);
        theQA.getResourceMetadata().put(ResourceMetadataKeyEnum.UPDATED, InstantDt.withCurrentTime());

        Deque<Observation> existingVersions = myIdToQVersions.get(theId);

        // We just use the current number of versions as the next version number
        String newVersion = Integer.toString(existingVersions.size());

        // Create an ID with the new version and assign it back to the resource
        IdDt newId = new IdDt("QuestionnaireAnswers", theId, newVersion);
        theQA.setId(newId);
        existingVersions.add(theQA);
    }

}
