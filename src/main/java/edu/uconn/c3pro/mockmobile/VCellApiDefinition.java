package edu.uconn.c3pro.mockmobile;
import com.github.scribejava.core.builder.api.DefaultApi20;

public class VCellApiDefinition extends DefaultApi20 {

    protected VCellApiDefinition() {
    }

    private static class InstanceHolder {
        private static final VCellApiDefinition INSTANCE = new VCellApiDefinition();
    }

    public static VCellApiDefinition instance() {
        return InstanceHolder.INSTANCE;
    }

	@Override
	public String getAccessTokenEndpoint() {
		return "http://localhost:8081/oauth/token";
	}

	@Override
	protected String getAuthorizationBaseUrl() {
		return "http://localhost:8081/oauth/authorize";
	}

}
