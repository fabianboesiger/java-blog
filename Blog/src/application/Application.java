package application;

import java.io.IOException;
import java.util.HashMap;

import database.Database;
import database.Messages;
import mailer.Mailer;
import server.Request;
import server.Responder;
import server.Server;

public class Application {
	
	private Database database;
	private Responder responder;
	private Mailer mailer;
	private Server server;
	
	private HashMap <String, Object> predefined = new HashMap <String, Object>();

	
	public Application() throws IOException {		
		database = new Database();
		responder = new Responder(predefined);
		mailer = new Mailer(predefined);
		server = new Server(responder, 8000);
		setup();
	}
	
	public void setup() throws IOException {
		
		predefined.put("title", "Fälis Blog");

		server.on("ALL", ".*", (Request request) -> {
			predefined.put("active-sessions", "" + server.activeCount());
			predefined.put("username", request.session.getUsername());
			return responder.next();
		});
		
		server.on("GET", "/", (Request request) -> {
			return responder.render("index.html", request.languages);
		});
		
		server.on("GET", "/articles", (Request request) -> {
			return responder.render("projects.html", request.languages);
		});
		
		server.on("GET", "/server", (Request request) -> {
			return responder.render("server.html", request.languages);
		});
		
		server.on("GET", "/stats", (Request request) -> {
			long uptimeMillis = server.uptime();
			
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			variables.put("uptime", String.format("%02d", uptimeMillis/1000/60/60) + ":" + String.format("%02d", uptimeMillis/1000/60%60) + ":" + String.format("%02d", uptimeMillis/1000%60));
			variables.put("sessions", "" + server.sessionsCount());
			variables.put("active-sessions", "" + server.activeCount());
			variables.put("handles-per-day", "" + Math.round(server.handlesPerDay()));
			
			return responder.render("stats.html", request.languages, variables);
		});
		
		server.on("ALL", "/profile.*", (Request request) -> {
			if(request.session.getUsername() == null) {
				return responder.redirect("/signin");
			} else {
				return responder.next();
			}
		});
		
		server.on("GET", "/profile", (Request request) -> {			
			return responder.render("profile.html", request.languages);
		});
		
		server.on("GET", "/profile/email", (Request request) -> {
			return responder.render("email.html", request.languages);
		});
		
		server.on("GET", "/profile/password", (Request request) -> {
			return responder.render("password.html", request.languages);
		});
		
		server.on("GET", "/profile/delete", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			Messages messages = (Messages) request.session.getFlash("errors");
			if(messages != null) {
				messages.addToVariables(variables, "errors");
			}
			
			return responder.render("delete.html", request.languages, variables);
		});
		
		server.on("GET", "/profile/delete/confirm", (Request request) -> {
			if(database.delete(User.class, request.session.getUsername())) {
				request.session.logout();
				return responder.redirect("/");
			} else {
				Messages messages = new Messages();
				messages.add("user", "deletion-error");
				request.session.addFlash("errors", messages);
				return responder.redirect("/profile/delete");
			}
			
		});
		
		server.on("GET", "/signup", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			
			Messages messages = (Messages) request.session.getFlash("errors");
			if(messages != null) {
				messages.addToVariables(variables, "errors");
			}

			return responder.render("signup.html", request.languages, variables);
		});
		
		server.on("POST", "/signup", (Request request) -> {
			User user = new User();
			user.parseFromMap(request.parameters);
			
			Messages messages = new Messages();
			
			if(user.validate(messages)) {
				if(database.save(user)) {
					request.session.login(user.getUsername());
					
					HashMap <String, Object> variables = new HashMap <String, Object> ();
					variables.put("username", user.getUsername());
					mailer.send(user.getMail(), "{{print translate \"signup\"}}", "signup.html", request.languages, variables);
					
					return responder.redirect("/");
				} else {
					messages.add("username", "in-use");
				}
			}
			
			request.session.addFlash("errors", messages);
			return responder.redirect("/signup");
		});
		
		server.on("GET", "/signin", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();

			Messages messages = (Messages) request.session.getFlash("errors");
			if(messages != null) {
				messages.addToVariables(variables, "errors");
			}
			
			return responder.render("signin.html", request.languages, variables);
		});
		
		server.on("POST", "/signin", (Request request) -> {
			Messages messages = new Messages();
			User user = null;
			
			if((user = (User) database.load(User.class, request.parameters.get("username"))) != null) {
				if(user.authenticate(request.parameters.get("password"))) {
					request.session.login(user.getUsername());
					return responder.redirect("/");
				} else {
					messages.add("password", "does-not-match");
				}
			} else {
				messages.add("user", "does-not-exist");
			}
			
			request.session.addFlash("errors", messages);
			return responder.redirect("/signin");
		});
		
		server.on("GET", "/signout", (Request request) -> {
			request.session.logout();
			return responder.redirect("/");
		});
		
	}
	
}
