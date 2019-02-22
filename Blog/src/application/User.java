package application;

import database.templates.ObjectTemplate;
import database.templates.StringTemplate;

public class User extends ObjectTemplate {
	
	private StringTemplate username;
	private StringTemplate password;
	private StringTemplate email;
	
	public User() {
		super("user");
		username = new StringTemplate("username", 2, 16);
		password = new StringTemplate("password", 4, 32);
		email = new StringTemplate("email", 0, 64);
		setId(username);
	}
	
	public boolean authenticate(String password) {
		if(password.equals(this.password.get())) {
			return true;
		}
		return false;
	}

}
