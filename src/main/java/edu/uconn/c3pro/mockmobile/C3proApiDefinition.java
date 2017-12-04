package edu.uconn.c3pro.mockmobile;
import com.github.scribejava.core.builder.api.ClientAuthenticationType;
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
	public ClientAuthenticationType getClientAuthenticationType() {
		// this is the default for DefaultApi20, but just to make it explicit
		return ClientAuthenticationType.HTTP_BASIC_AUTHENTICATION_SCHEME;
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "http://localhost:8081/c3pro/auth";
	}

	@Override
	protected String getAuthorizationBaseUrl() {
		return "http://localhost:8082/oauth/authorize";
	}	
	

}
