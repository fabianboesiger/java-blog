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
		username = new IdentifiableStringTemplate("username", 2, 16);
		password = new StringTemplate("password", 4, 32);
		email = new StringTemplate("email", 0, 64);
		setIdentifier(username);
	}
	
	public boolean authenticate(String password) {
		if(password.equals(this.password.get())) {
			return true;
		}
		return false;
	}

}
