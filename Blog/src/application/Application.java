package application;

import java.io.File;
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
		responder = new Responder(predefined, new File("views/web"));
		mailer = new Mailer(predefined, new File("views/mail"));
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
			double bytesPerDay = server.bytesPerDay();
			String formatted = null;
			for(int i = 3; i >= 0; i--) {
				double power = Math.pow(1000, i);
				if(bytesPerDay >= power) {
					formatted = String.format("%.3f", (bytesPerDay / power)) + " ";
					switch(i) {
					case 0:
						formatted += "B";
						break;
					case 1:
						formatted += "KB";
						break;
					case 2:
						formatted += "MB";
						break;
					case 3:
						formatted += "GB";
						break;
					}
					break;
				}
			}
			if(formatted == null) {
				formatted = String.format("%.3f", bytesPerDay) + " B";
			}
			
			variables.put("bytes-per-day", "" + formatted);
			
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
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			Messages messages = (Messages) request.session.getFlash("errors");
			if(messages != null) {
				messages.addToVariables(variables, "errors");
			}
			
			return responder.render("email.html", request.languages, variables);
		});
		
		server.on("POST", "/profile/email", (Request request) -> {
			Messages messages = new Messages();
			User user = null;
			if((user = (User) database.load(User.class, request.session.getUsername())) != null) {
				user.setMail(request.parameters.get("email"));
				if(user.validate(messages)) {
					if(database.update(user)) {
						return responder.redirect("/profile");
					}
				}
			} else {
				messages.add("user", "does-not-exist");
			}
			
			request.session.addFlash("errors", messages);
			return responder.redirect("/profile/email");
		});
		
		server.on("GET", "/profile/password", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			Messages messages = (Messages) request.session.getFlash("errors");
			if(messages != null) {
				messages.addToVariables(variables, "errors");
			}
			
			return responder.render("password.html", request.languages, variables);
		});
		
		server.on("POST", "/profile/password", (Request request) -> {
			Messages messages = new Messages();
			User user = null;
			if((user = (User) database.load(User.class, request.session.getUsername())) != null) {
				user.setPassword(request.parameters.get("password"));
				if(user.validate(messages)) {
					if(database.update(user)) {
						return responder.redirect("/profile");
					}
				}
			} else {
				messages.add("user", "does-not-exist");
			}
			
			request.session.addFlash("errors", messages);
			return responder.redirect("/profile/password");
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
			user.parseFromParameters(request.parameters);

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
		
		server.on("GET", "/recover", (Request request) -> {			
			return responder.render("recover.html", request.languages);
		});
		
	}
	
}
