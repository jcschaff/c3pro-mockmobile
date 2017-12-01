package edu.uconn.c3pro.mockmobile;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.hl7.fhir.instance.model.Questionnaire;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.httpclient.ahc.AhcHttpClient;
import com.github.scribejava.httpclient.ahc.AhcHttpClientConfig;
import com.google.gson.Gson;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class Client {
	private static final String NETWORK_NAME = "Facebook";
	private static final String PROTECTED_RESOURCE_URL = "http://localhost:8082/graph.facebook.com/me";
	private static final Token EMPTY_TOKEN = null;
	
	public static void main(String[] args) {
		
		try { 
		    // Replace these with your own api key and secret
		    String apiKey = "your_app_id";
		    String apiSecret = "your_api_secret";
		    
	        final AhcHttpClientConfig clientConfig = new AhcHttpClientConfig(new DefaultAsyncHttpClientConfig.Builder()
	                .setMaxConnections(5)
	                .setRequestTimeout(10000)
	                .setPooledConnectionIdleTimeout(1000)
	                .setReadTimeout(1000)
	                .build());

		    //AhcHttpClient client = new AhcHttpClient(AhcHttpClientConfig.defaultConfig());
	        AhcHttpClient client = new AhcHttpClient(clientConfig);
	        String userAgent = "c3pro-java-client/1.0";
			Map<String, String> headers = new HashMap<String,String>(){{
				put("Antispam", "MY-ANTI-SPAM");
			}};
			Gson gson = new Gson();
			Registration registration = new Registration(true,"your apple-supplied app purchase receipt");
			String registrationJSON = gson.toJson(registration);
			String registrationJSON_expected = "{\n" + 
					"      \"sandbox\": true,\n" + 
					"      \"receipt-data\": \"your apple-supplied app purchase receipt\"\n" + 
					"    }";
			System.out.println("registrationJSON:\n"+registrationJSON+"\n\nexpected:\n"+registrationJSON_expected+"\n");
			byte[] bodyContents = registrationJSON.getBytes("UTF-8");
			final String REGISTRATION_URL = "http://localhost:8081/c3pro/register";
			Response response = client.execute(userAgent, headers, Verb.POST, REGISTRATION_URL, bodyContents);
			String registrationResponseJSON = response.getBody();
			RegistrationResponse registrationResponse = gson.fromJson(registrationResponseJSON, RegistrationResponse.class);
			
/*		    HTTP/1.1 POST /c3pro/oauth?grant_type=client_credentials
		    	    Authentication: Basic BASE64(ClientId:Secret)
		    	NOTE: According to OAuth2 two-legged specifications both clientId and Secret should be x-www-form-urlencoded before Base64 encoding is applied.

		    	Oauth2 authorization response

		    	    HTTP/1.1 201 Created
		    	    Content-Type: application/json
		    	    {
		    	      "access_token":"{{some token}}",
		    	      "expires_in": "{{seconds to expiration}}",
		    	      "token_type": "bearer"
		    	    } 
*/			
		    try (OAuth20Service service = new ServiceBuilder(apiKey)
		                                  .apiKey(apiKey)
		                                  .apiSecret(apiSecret)
		                                  .debug()
		                                  .callback("http://localhost:8080/oauth_callback/")
		                                  .httpClientConfig(clientConfig)
		                                  .build(C3proApiDefinition.instance());){
			    Scanner in = new Scanner(System.in);
	
			    OAuth2AccessToken accessToken = service.getAccessTokenClientCredentialsGrant();
			    
			    
			    FhirContext ctx = FhirContext.forDstu2();
			    IParser fhirParser = ctx.newJsonParser();
			    
			    //
			    // retrieve questionnaire "q1"
			    //
			    final OAuthRequest request = new OAuthRequest(Verb.GET, "https://localhost:8082/c3pro/fhir/Questionnaire/q1");
			    service.signRequest(accessToken, request);
			    final Response questionnaireResponse = service.execute(request);
			    if (questionnaireResponse.getCode() != 200) {
			    		throw new RuntimeException("unexpected return code "+questionnaireResponse.getCode()+" - "+questionnaireResponse.getMessage());
			    }
			    final String questionnaireString = response.getBody();
			    final Questionnaire questionnaire = fhirParser.parseResource(Questionnaire.class, questionnaireString);
			    System.out.println("received questionnaire: "+fhirParser.encodeResourceToString(questionnaire));
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
