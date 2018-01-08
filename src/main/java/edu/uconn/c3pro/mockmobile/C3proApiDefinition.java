package edu.uconn.c3pro.mockmobile;
import com.github.scribejava.core.builder.api.ClientAuthenticationType;
import com.github.scribejava.core.builder.api.DefaultApi20;

public class C3proApiDefinition extends DefaultApi20 {
	private final String baseurl_auth;

    protected C3proApiDefinition(String baseurl_auth) {
    		this.baseurl_auth = baseurl_auth;
    }

	@Override
	public ClientAuthenticationType getClientAuthenticationType() {
		// this is the default for DefaultApi20, but just to make it explicit
		return ClientAuthenticationType.HTTP_BASIC_AUTHENTICATION_SCHEME;
	}

	@Override
	public String getAccessTokenEndpoint() {
		return baseurl_auth+"/c3pro/auth";
	}

	@Override
	protected String getAuthorizationBaseUrl() {
		return baseurl_auth+"/c3pro/register";
	}	
	

}
