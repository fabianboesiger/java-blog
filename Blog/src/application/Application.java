package application;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.Database;
import database.DatabaseException;
import database.templates.Errors;
import server.Request;
import server.Responder;
import server.Server;

public class Application {
	
	private Database database;
	private Responder responder;
	private Server server;
	
	private HashMap <String, Object> predefined = new HashMap <String, Object>();

	
	public Application() throws IOException, DatabaseException {		
		database = new Database();
		responder = new Responder(predefined);
		server = new Server(responder);
		setup();
		
	}
	
	public void setup() throws IOException {
		
		predefined.put("title", "Fälis Blog");

		server.on("ALL", ".*", (Request request) -> {
			predefined.put("active-sessions", "" + server.activeCount());
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
			user.setFromStringMap(request.parameters);
			Errors errors = new Errors();
			if(user.validate(errors)) {
				//if(database.save(user)) {
					return responder.text("success");
				//} else {
					//errors.add("username", "in-use");
				//}
			}
			request.session.addFlash("errors", errors);
			return responder.redirect("/signup");
		});
		
	}
	
}
