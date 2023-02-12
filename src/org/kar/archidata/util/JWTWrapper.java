package org.kar.archidata.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class JWTWrapper {
	private static RSAKey rsaJWK = null;;
	private static RSAKey rsaPublicJWK = null;

	public static class PublicKey {
		public String key;

		public PublicKey(String key) {
			this.key = key;
		}
		public PublicKey() {
		}
	}
    public static void initLocalTokenRemote(String ssoUri, String application) throws IOException, ParseException {
        // check Token:
        URL obj = new URL(ssoUri + "public_key");
        System.out.println("Request token from:" + obj);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", application);
        con.setRequestProperty("Cache-Control", "no-cache");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        String ssoToken = ConfigBaseVariable.ssoToken();
        if (ssoToken != null) {
        	con.setRequestProperty("Authorization", "Zota " + ssoToken);
        }
        int responseCode = con.getResponseCode();

        System.out.println("GET Response Code :: " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            // print result
            //System.out.println(response.toString());
            ObjectMapper mapper = new ObjectMapper();
            PublicKey values = mapper.readValue(response.toString(), PublicKey.class);
            rsaPublicJWK = RSAKey.parse(values.key);
            return;
        }
        System.out.println("GET JWT validator token not worked");
    }

	public static void initLocalToken() throws Exception{
		// RSA signatures require a public and private RSA key pair, the public key 
		// must be made known to the JWS recipient in order to verify the signatures
		try {
			String generatedStringForKey = UUID.randomUUID().toString();
			rsaJWK = new RSAKeyGenerator(2048).keyID(generatedStringForKey).generate();
			rsaPublicJWK = rsaJWK.toPublicJWK();
			//System.out.println("RSA key (all): " + rsaJWK.toJSONString());
			//System.out.println("RSA key (pub): " + rsaPublicJWK.toJSONString());
		} catch (JOSEException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Can not generate teh  public abnd private keys ...");
			rsaJWK = null;
			rsaPublicJWK = null;
		}
	}
	
	public static void initValidateToken(String publicKey) {
		try {
			rsaPublicJWK = RSAKey.parse(publicKey);
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("Can not retrieve public Key !!!!!!!! RSAKey='" + publicKey + "'");
		}
		
	}
	public static String getPublicKeyJson() {
		if (rsaPublicJWK == null) {
			return null;
		}
		return rsaPublicJWK.toJSONString();
	}
	public static java.security.interfaces.RSAPublicKey getPublicKeyJava() throws JOSEException {
		if (rsaPublicJWK == null) {
			return null;
		}
		// Convert back to std Java interface
		return rsaPublicJWK.toRSAPublicKey();
	}
	
	/**
	 * Create a token with the provided elements
	 * @param userID UniqueId of the USER (global unique ID)
	 * @param userLogin Login of the user (never change)
	 * @param isuer The one who provide the Token
	 * @param timeOutInMunites Expiration of the token.
	 * @return the encoded token
	 */
	public static String generateJWToken(long userID, String userLogin, String isuer, String application, int timeOutInMunites) {
		if (rsaJWK == null) {
			System.out.println("JWT private key is not present !!!");
			return null;
		}
		/*
		System.out.println(" ===> expire in : " + timeOutInMunites);
		System.out.println(" ===>" + new Date().getTime());
		System.out.println(" ===>" + new Date(new Date().getTime()));
		System.out.println(" ===>" + new Date(new Date().getTime() - 60 * timeOutInMunites * 1000));
		*/
		try {
			// Create RSA-signer with the private key
			JWSSigner signer = new RSASSASigner(rsaJWK);
			// Prepare JWT with claims set
			JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			    .subject(Long.toString(userID))
			    .claim("login", userLogin)
			    .claim("application", application)
			    .issuer(isuer)
			    .issueTime(new Date())
			    .expirationTime(new Date(new Date().getTime() + 60 * timeOutInMunites * 1000 /* millisecond */)) // Do not ask why we need a "-" here ... this have no meaning
			    .build();
	
			SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT)/*.keyID(rsaJWK.getKeyID())*/.build(), claimsSet);
	
			// Compute the RSA signature
			signedJWT.sign(signer);
			// serialize the output...
			return signedJWT.serialize();
		} catch (JOSEException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static JWTClaimsSet validateToken(String signedToken, String isuer, String application) {
		if (rsaPublicJWK == null) {
			System.out.println("JWT public key is not present !!!");
			return null;
		}
		try {
			// On the consumer side, parse the JWS and verify its RSA signature
			SignedJWT signedJWT = SignedJWT.parse(signedToken);
	
			JWSVerifier verifier = new RSASSAVerifier(rsaPublicJWK);
			if (!signedJWT.verify(verifier)) {
				System.out.println("JWT token is NOT verified ");
				return null;
			}
			if (!new Date().before(signedJWT.getJWTClaimsSet().getExpirationTime())) {
				System.out.println("JWT token is expired now = " + new Date() + " with=" + signedJWT.getJWTClaimsSet().getExpirationTime() );
				return null;
			}
			if (!isuer.equals(signedJWT.getJWTClaimsSet().getIssuer())) {
				System.out.println("JWT issuer is wong: '" + isuer + "' != '" + signedJWT.getJWTClaimsSet().getIssuer() + "'" );
				return null;
			}
			if (application != null) {
				// TODO: verify the token is used for the correct application.
			}
			// the element must be validated outside ...
			//System.out.println("JWT token is verified 'alice' =?= '" + signedJWT.getJWTClaimsSet().getSubject() + "'");
			//System.out.println("JWT token isuer 'https://c2id.com' =?= '" + signedJWT.getJWTClaimsSet().getIssuer() + "'");
			return signedJWT.getJWTClaimsSet();
		} catch (JOSEException ex) {
			ex.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
}
