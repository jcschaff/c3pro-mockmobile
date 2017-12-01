package edu.uconn.c3pro.mockmobile;
import com.github.scribejava.core.builder.api.DefaultApi20;

public class C3proApiDefinition extends DefaultApi20 {

    protected C3proApiDefinition() {
    }

    private static class InstanceHolder {
        private static final C3proApiDefinition INSTANCE = new C3proApiDefinition();
    }

    public static C3proApiDefinition instance() {
        return InstanceHolder.INSTANCE;
    }

	@Override
	public String getAccessTokenEndpoint() {
		return "http://localhost:8081/c3pro/oauth";
	}

	@Override
	protected String getAuthorizationBaseUrl() {
		return "http://localhost:8081/oauth/authorize";
	}	
	

}
