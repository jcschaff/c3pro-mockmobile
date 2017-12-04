package edu.uconn.c3pro.mockmobile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.filter.FilterContext;
import org.asynchttpclient.filter.FilterException;
import org.asynchttpclient.filter.RequestFilter;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.httpclient.ahc.AhcHttpClient;
import com.github.scribejava.httpclient.ahc.AhcHttpClientConfig;
import com.google.gson.Gson;

public class Client {
	
	public static void main(String[] args) {
		
		try { 
			Gson gson = new Gson();

			RequestFilter requestFilter = new RequestFilter() {
				@Override
				public <T> FilterContext<T> filter(FilterContext<T> arg0) throws FilterException {
					System.out.println("intercepting request url "+arg0.getRequest().getUrl());
					return arg0;
				}
			};
			//
			// must match antispam token in auth/resource database
			// (check debug log of auth server for token to add to database)
			//
			String antispam = "myantispam";
			
			final AhcHttpClientConfig registrationClientConfig = new AhcHttpClientConfig(new DefaultAsyncHttpClientConfig.Builder()
	                .setMaxConnections(5)
	                .setRequestTimeout(10000)
	                .setPooledConnectionIdleTimeout(1000)
	                .addRequestFilter(requestFilter)
	                .setReadTimeout(1000)
	                .setDisableUrlEncodingForBoundRequests(true)
	                .addRequestFilter(new RequestFilter() {
						@Override
						public <T> FilterContext<T> filter(FilterContext<T> ctx) throws FilterException {
							ctx.getRequest().getHeaders().add("Antispam", antispam);
							return ctx;
						}
					})
	                .build());

	        String userAgent = "c3pro-java-client/1.0";
			Map<String, String> headers = new HashMap<String,String>(){{
				put("Content-Type", "application/json");
				put("Accept","application/json");
			}};
			Registration registration = new Registration(true,"your apple-supplied app purchase receipt");
			String registrationJSON = gson.toJson(registration);
			byte[] bodyContents = registrationJSON.getBytes("UTF-8");
			final String REGISTRATION_URL = "http://localhost:8081/c3pro/register";
			
			Response response = null;
	        try (AhcHttpClient client = new AhcHttpClient(registrationClientConfig);){
		        	System.out.println("before client.execute(/c3pro/register)");
		        	response = client.execute(userAgent, headers, Verb.POST, REGISTRATION_URL, bodyContents);
		        	System.out.println("after client.execute(/c3pro/register)");
	        }
	        
	        	String registrationResponseJSON = response.getBody();
	        	RegistrationResponse registrationResponse = gson.fromJson(registrationResponseJSON, RegistrationResponse.class);
	        	System.out.println("registration gotten back from server: "+gson.toJson(registrationResponse));

			String urlEncodedClientId = URLEncoder.encode(registrationResponse.getClient_id(),"UTF-8");
			String urlEncodedClientSecret = URLEncoder.encode(registrationResponse.getClient_secret(),"UTF-8");

			
	        	//
	        	// manually create the authentication token from client_id and client_secret
	        	// this is not used currently, as the same functionality is embedded in the 
	        	//
//			String bearerToken = urlEncodedClientId+":"+urlEncodedClientSecret;
//			String encodedBearerToken = Base64.getEncoder().encodeToString(bearerToken.getBytes("UTF-8"));
//			System.out.println("manual header:  'Basic "+encodedBearerToken);
			
			final AhcHttpClientConfig authorizationClientConfig = new AhcHttpClientConfig(new DefaultAsyncHttpClientConfig.Builder()
	                .setMaxConnections(5)
	                .setRequestTimeout(10000)
	                .setPooledConnectionIdleTimeout(1000)
	                .addRequestFilter(requestFilter)
	                .setReadTimeout(1000)
	                .setDisableUrlEncodingForBoundRequests(true)
	                .addRequestFilter(new RequestFilter() {
						@Override
						public <T> FilterContext<T> filter(FilterContext<T> ctx) throws FilterException {
						//	ctx.getRequest().getHeaders().add("Authorization", "Basic"+ " " + encodedBearerToken);
							ctx.getRequest().getHeaders().add("Antispam", antispam);
							return ctx;
						}
					})
	                .build());

			OutputStream debugStream = System.out;
		    try (OAuth20Service service = new ServiceBuilder(urlEncodedClientId)
		                                  .apiKey(urlEncodedClientId)
		                                  .apiSecret(urlEncodedClientSecret)
		                                  .debug()
		                                  .debugStream(debugStream)
		                                  .httpClientConfig(authorizationClientConfig)
		                                  .build(C3proApiDefinition.instance());){
		    	
		    		System.out.println("before service.getAccessTokenClientCredentialsGrant()");
			    OAuth2AccessToken accessToken = service.getAccessTokenClientCredentialsGrant();
			    	System.out.println("after service.getAccessTokenClientCredentialsGrant()");
			    
				System.out.println("accessToken: "+accessToken.getAccessToken());
				System.out.println("refreshToken: "+accessToken.getRefreshToken());
				System.out.println("expiresIn: "+accessToken.getExpiresIn());
				
				//
				// next, we'll exercise the resource server using the same "service".
				//
				
//			    FhirContext ctx = FhirContext.forDstu2Hl7Org();
//			    IParser fhirParser = ctx.newJsonParser();
//			    
//			    //
//			    // retrieve questionnaire "q1"
//			    //
//			    final OAuthRequest request = new OAuthRequest(Verb.GET, "https://localhost:8082/c3pro/fhir/Questionnaire/q1");
//			    service.signRequest(accessToken, request);
//			    final Response questionnaireResponse = service.execute(request);
//			    if (questionnaireResponse.getCode() != 200) {
//			    		throw new RuntimeException("unexpected return code "+questionnaireResponse.getCode()+" - "+questionnaireResponse.getMessage());
//			    }
//			    final String questionnaireString = response.getBody();
//			    final Questionnaire questionnaire = fhirParser.parseResource(Questionnaire.class, questionnaireString);
//			    System.out.println("received questionnaire: "+fhirParser.encodeResourceToString(questionnaire));
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
