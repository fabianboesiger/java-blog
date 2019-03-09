package application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import database.templates.IdentifiableStringTemplate;
import database.templates.ObjectTemplate;
import database.templates.StringTemplate;

public class User extends ObjectTemplate {
		
	private IdentifiableStringTemplate username;
	private StringTemplate password;
	private StringTemplate email;
	
	public User() {
		this(null);
	}
	
	public User(String name) {
		super(name);
		
		username = new IdentifiableStringTemplate("username", 2, 16);
		password = new StringTemplate("password", 4, 32, (Object value) -> {
			return hash((String) value);
		});
		email = new StringTemplate("email", 0, 64);
		setIdentifier(username);
	}
	
	public boolean authenticate(String password) {
		if(password != null) {
			try {
				if(hash(password).equals(this.password.get())) {
					return true;
				}
			} catch(NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public String getUsername() {
		return (String) username.get();
	}
	
	public static String hash (String input) throws NoSuchAlgorithmException {
		if(input == null) {
			return null;
		}
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
		messageDigest.update(input.getBytes());
		byte[] bytes = messageDigest.digest();
		StringBuilder stringBuilder = new StringBuilder();
		for(int i = 0; i < bytes.length ; i++) {
			stringBuilder.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return stringBuilder.toString();
	}

	public String getMail() {
		return (String) email.get();
	}

	public void setPassword(String string) {
		password.set(string);
	}

	public void setMail(String string) {
		email.set(string);
	}

}
