package application;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.Database;
import database.templates.Errors;
import server.Request;
import server.Responder;
import server.Server;

public class Application {
	
	private Database database;
	private Responder responder;
	private Server server;
	
	private HashMap <String, Object> predefined = new HashMap <String, Object>();

	
	public Application() throws IOException {		
		database = new Database();
		responder = new Responder(predefined);
		server = new Server(responder);
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
		
		server.on("GET", "/projects", (Request request) -> {
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
			variables.put("handles-per-hour", "" + server.handlesPerHour());
			variables.put("visitors-per-hour", "" + server.visitorsPerHour());
			return responder.render("stats.html", request.languages, variables);
		});
		
		server.on("GET", "/signup", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			Errors errors = (Errors) request.session.getFlash("errors");
			if(errors != null) {
				List <Map <String, String>> list = errors.get();
				if(list.size() > 0) {
					variables.put("errors", list);
				}
			}
			return responder.render("signup.html", request.languages, variables);
		});
		
		server.on("POST", "/signup", (Request request) -> {
			User user = new User();
			user.parse(request.parameters);
			Errors errors = new Errors();
			if(user.validate(errors)) {
				if(database.save(user, false)) {
					request.session.login(user.getUsername());
					return responder.redirect("success");
				} else {
					errors.add("username", "in-use");
				}
			}
			request.session.addFlash("errors", errors);
			return responder.redirect("/signup");
		});
		
		server.on("GET", "/signin", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			Errors errors = (Errors) request.session.getFlash("errors");
			if(errors != null) {
				List <Map <String, String>> list = errors.get();
				if(list.size() > 0) {
					variables.put("errors", list);
				}
			}
			return responder.render("signin.html", request.languages, variables);
		});
		
		server.on("POST", "/signin", (Request request) -> {
			User user = new User();
			Errors errors = new Errors();
			if(database.load(user, request.parameters.get("username"))) {
				if(user.authenticate(request.parameters.get("password"))) {
					request.session.login(user.getUsername());
					return responder.redirect("/");
				} else {
					errors.add("password", "does-not-match");
				}
			} else {
				errors.add("user", "does-not-exist");
			}
			request.session.addFlash("errors", errors);
			return responder.redirect("/signin");
		});
		
		server.on("GET", "/signout", (Request request) -> {
			request.session.logout();
			return responder.redirect("/");
		});
		
	}
	
}
