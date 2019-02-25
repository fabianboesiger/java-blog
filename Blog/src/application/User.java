package application;

import database.templates.IdentifiableStringTemplate;
import database.templates.ObjectTemplate;
import database.templates.StringTemplate;

public class User extends ObjectTemplate {
	
	private IdentifiableStringTemplate username;
	private StringTemplate password;
	private StringTemplate email;
	
	public User() {
		super("user");
		username = new IdentifiableStringTemplate("username", 2, 16, (Object value) -> {
			return hash((String) value);
		});
		password = new StringTemplate("password", 4, 32);
		email = new StringTemplate("email", 0, 64);
		setIdentifier(username);
	}
	
	public boolean authenticate(String password) {
		if(password != null) {
			if(hash(password).equals(this.password.get())) {
				return true;
			}
		}
		return false;
	}

	public String getUsername() {
		return (String) username.get();
	}
	
	private static String hash(String input) {
		return input;
	}

}
