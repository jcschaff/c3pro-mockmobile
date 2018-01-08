package edu.uconn.c3pro.mockmobile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.TrustManagerFactory;

import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.filter.FilterContext;
import org.asynchttpclient.filter.FilterException;
import org.asynchttpclient.filter.RequestFilter;
import org.asynchttpclient.netty.ssl.DefaultSslEngineFactory;
import org.bch.c3pro.server.exception.C3PROException;
import org.bch.c3pro.server.external.Queue;
import org.bch.c3pro.server.iresource.ConsentResourceProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.httpclient.ahc.AhcHttpClient;
import com.github.scribejava.httpclient.ahc.AhcHttpClientConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.uhn.fhir.model.dstu2.resource.Contract;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

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
	        String baseurl_auth = "https://api.jcschaff.net:8888";
            //String baseurl_auth = "https://2amoveu7z2.execute-api.us-east-1.amazonaws.com/alpha";
            //String baseurl_auth = "https://api.jcschaff.net/c3pro/alpha";
            String baseurl_api = "https://api.jcschaff.net:8889";
            //String baseurl_auth = "https://2amoveu7z2.execute-api.us-east-1.amazonaws.com/alpha";
            //String baseurl_auth = "https://api.jcschaff.net/c3pro/alpha";
	         			
            String antispam = "myantispam";
			TrustManagerFactory trustManagerFactory = InsecureTrustManagerFactory.INSTANCE;
			SslContextBuilder insecureSslBuilder = SslContextBuilder.forClient().trustManager(trustManagerFactory);
			SslContext sslContext = insecureSslBuilder.build();
			
			final AhcHttpClientConfig registrationClientConfig = new AhcHttpClientConfig(new DefaultAsyncHttpClientConfig.Builder()
	                .setMaxConnections(5)
	                .setSslContext(sslContext)
	                .setSslEngineFactory(new DefaultSslEngineFactory())
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
			Registration registration = new Registration(true,"NO-APP-RECEIPT");
			ObjectMapper objectMapper = new ObjectMapper();
			String registrationJSON = objectMapper.writeValueAsString(registration);
			//String registrationJSON = gson.toJson(registration);
			byte[] bodyContents = registrationJSON.getBytes("UTF-8");
			final String REGISTRATION_URL = baseurl_auth+"/c3pro/register";
			
			Response response = null;
	        try (AhcHttpClient client = new AhcHttpClient(registrationClientConfig);){
		        	System.out.println("before client.execute(/c3pro/register)");
		        	response = client.execute(userAgent, headers, Verb.POST, REGISTRATION_URL, bodyContents);
		        	System.out.println("after client.execute(/c3pro/register)");
	        }
	        
	        	String registrationResponseJSON = response.getBody();
	        if (	HttpResponseStatus.valueOf(response.getCode()) != HttpResponseStatus.CREATED) {
	        		System.err.println(toPrettyFormat(response.getBody()));
	        		throw new RuntimeException("return code '"+HttpResponseStatus.valueOf(response.getCode())+"', unexpected during registration");
	        }
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
	                .setSslContext(sslContext)
	                .setSslEngineFactory(new DefaultSslEngineFactory())
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
		                                  .build(new C3proApiDefinition(baseurl_auth));){
		    	
		    		System.out.println("before service.getAccessTokenClientCredentialsGrant()");
			    OAuth2AccessToken accessToken = service.getAccessTokenClientCredentialsGrant();
			    	System.out.println("after service.getAccessTokenClientCredentialsGrant()");
			    
				System.out.println("accessToken: "+accessToken.getAccessToken());
				System.out.println("refreshToken: "+accessToken.getRefreshToken());
				System.out.println("expiresIn: "+accessToken.getExpiresIn());
				
				//
				// next, we'll exercise the resource server using the same "service".
				//
				ConsentResourceProvider consentProvider = new ConsentResourceProvider(
						new PostQueue(baseurl_api+"/c3pro/fhirenc/Contract",service,accessToken)
				);
				Contract contract = new Contract();
				consentProvider.createContract(contract);
			    
			    //
			    // retrieve questionnaire "q1"
			    //
				{
//			    FhirContext ctx = FhirContext.forDstu2();   // Dstu2Hl7Org();
//			    IParser fhirParser = ctx.newJsonParser();
//			    final OAuthRequest request = new OAuthRequest(Verb.GET, baseurl_api+"/c3pro/fhir/Questionnaire/q1");
//			    service.signRequest(accessToken, request);
//			    final Response questionnaireResponse = service.execute(request);
//			    if (questionnaireResponse.getCode() != 200) {
//			    		throw new RuntimeException("unexpected return code "+questionnaireResponse.getCode()+" - "+questionnaireResponse.getMessage());
//			    }
//			    final String questionnaireString = response.getBody();
//			    final Questionnaire questionnaire = fhirParser.parseResource(Questionnaire.class, questionnaireString);
//			    System.out.println("received questionnaire: "+fhirParser.encodeResourceToString(questionnaire));
				}
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static class PostQueue implements Queue {
		final String url;
		final OAuth20Service service;
		final OAuth2AccessToken accessToken;
		
		
		public PostQueue(String url, OAuth20Service service, OAuth2AccessToken accessToken) {
			this.url = url;
			this.service = service;
			this.accessToken = accessToken;
		}

		@Override
		public void sendMessageEncrypted(String resource, PublicKey key, String UUIDKey, String version)
				throws C3PROException {
			try {
			    MessageEncryptionService encryptionService = new MessageEncryptionService();
			    EncryptedMessage encryptedConsentMessage = encryptionService.encryptMessage(resource, key, UUIDKey, version);
				postEncrypted(service, accessToken, encryptedConsentMessage, url);
			} catch (InterruptedException | ExecutionException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private static void postEncrypted(OAuth20Service service, OAuth2AccessToken accessToken,
			EncryptedMessage encryptedConsentMessage, String url)
			throws JsonProcessingException, InterruptedException, ExecutionException, IOException {
		
		final OAuthRequest request = new OAuthRequest(Verb.POST, url);
		String encryptedConsentMessageJSON = new ObjectMapper().writeValueAsString(encryptedConsentMessage);
		request.setPayload(encryptedConsentMessageJSON);
		request.addHeader("Content-Type", "application/json");
		request.addHeader("Accept","application/json");
		service.signRequest(accessToken, request);
		final Response consentResponse = service.execute(request);
		if (consentResponse.getCode() != 200) {
			throw new RuntimeException("unexpected return code "+consentResponse.getCode()+" - "+consentResponse.getMessage());
		}
	}

	public static String toPrettyFormat(String jsonString) 
	  {
	      JsonParser parser = new JsonParser();
	      JsonObject json = parser.parse(jsonString).getAsJsonObject();

	      Gson gson = new GsonBuilder().setPrettyPrinting().create();
	      String prettyJson = gson.toJson(json);

	      return prettyJson;
	  }
}
