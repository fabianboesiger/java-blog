package application;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import database.Database;
import database.Messages;
import database.templates.ObjectTemplate;
import mailer.Mailer;
import server.Request;
import server.Responder;
import server.Server;

public class Application {
	
	private static final boolean PRODUCTION = true;
	
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
	
	private void setup() throws IOException {
		
		indexRoutes();
		serverRoutes();
		articleRoutes();
		signRoutes();
		recoveryRoutes();
		profileRoutes();
		
	}
	
	private void indexRoutes() {
		
		predefined.put("title", "Fälis Blog");
		if(PRODUCTION) {
			predefined.put("url", "http://blog.ddnss.ch");
		} else {
			predefined.put("url", "http://127.0.0.1:8000");
		}
		predefined.put("email", "faelisblog@gmail.com");

		server.on("ALL", ".*", (Request request) -> {
			predefined.put("username", request.session.getUsername());
			return responder.next();
		});
		
		server.on("GET", "/", (Request request) -> {
			return responder.render("index.html", request.languages);
		});
		
	}
	
	private void articleRoutes() {
		
		server.on("GET", "/articles", (Request request) -> {
			
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			
			int page = 0;
			int range = 8;
			if(request.parameters.containsKey("page")) {
				page = Integer.parseInt(request.parameters.get("page"));
			}
			
			boolean admin = false;
			
			User user = null;
			if((user = (User) database.load(User.class, request.session.getUsername())) != null) {
				variables.put("admin", user.isAdmin());
				admin = true;
			}
			
			LinkedList <ObjectTemplate> articleObjects = null;
			if(admin) {
				articleObjects = database.loadAll(Article.class);
			} else {
				articleObjects = database.loadAll(Article.class, (ObjectTemplate objectTemplate) -> {
					Article article = (Article) objectTemplate;
					return article.isVisible();
				});
			}
			LinkedList <HashMap <String, Object>> articles = new LinkedList <HashMap <String, Object>> ();
			
			if(articleObjects != null) {
				for(int i = 0; i < articleObjects.size(); i++) {
					if(articleObjects.size() - 1 - i >= page * range && articleObjects.size() - 1 - i < (page + 1) * range) {
						articles.addFirst(((Article) articleObjects.get(i)).getValues());
					}
				}
			}
			
			Integer previous = (page > 0) ? (page - 1) : null;
			Integer next = (articleObjects.size() > (page + 1) * range) ? (page + 1) : null;
			
			variables.put("previous", previous);
			variables.put("next", next);
			
			variables.put("articles", articles);
			
			return responder.render("articles/index.html", request.languages, variables);
		});
		
		server.on("GET", "/articles/create", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();

			Messages messages = (Messages) request.session.getFlash("errors");
			if(messages != null) {
				messages.addToVariables(variables, "errors");
			}

			return responder.render("articles/create.html", request.languages, variables);
		});
		
		server.on("POST", "/articles/create", (Request request) -> {
			User user = null;
			if((user = (User) database.load(User.class, request.session.getUsername())) != null) {
				Article article = new Article();
				article.parseFromParameters(request.parameters);
				article.setAuthor(user);

				Messages messages = new Messages();
				
				if(article.validate(messages)) {
					if(database.save(article)) {
						return responder.redirect("/articles");
					}
				}

				request.session.addFlash("errors", messages);
				return responder.redirect("/articles/create");
			}
			return responder.redirect("/signin");
		});
		
		server.on("GET", "/articles/article/(.*)", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();

			
			User user = null;
			if((user = (User) database.load(User.class, request.session.getUsername())) != null) {				
				variables.put("admin", user.isAdmin());
			}
			
			Article article = null;
			if((article = (Article) database.loadId(Article.class, request.groups.get(0))) != null) {
				variables.put("article", article.getValues());
				return responder.render("articles/article.html", request.languages, variables);
			} else {
				return responder.redirect("/articles");
			}
			
		});
		
	}
	
	private void serverRoutes() {
		
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
		
	}
	
	private void signRoutes() {
		
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
					
					sendActivationMail(user, request);
					
					return responder.redirect("/");
				} else {
					messages.add("username", "in-use");
				}
			}

			request.session.addFlash("errors", messages);
			return responder.redirect("/signup");
		});
		
		server.on("GET", "/signout", (Request request) -> {
			request.session.logout();
			return responder.redirect("/");
		});
		
	}
	
	private void recoveryRoutes() {
		
		server.on("GET", "/activate", (Request request) -> {	
			User user = null;
			if((user = (User) database.loadId(User.class, request.parameters.get("id"))) != null) {
				if(user.getKey().equals(request.parameters.get("key"))) {
					request.session.login(user.getUsername());
					user.setActivated(true);
					database.update(user);
					return responder.redirect("/profile");
				}
			}
			return responder.render("recover/activate.html", request.languages);
		});
		
		server.on("GET", "/recover", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();

			Messages messages = (Messages) request.session.getFlash("errors");
			if(messages != null) {
				messages.addToVariables(variables, "errors");
			}
			
			return responder.render("recover/index.html", request.languages, variables);
		});
		
		server.on("POST", "/recover", (Request request) -> {
			Messages messages = new Messages();
			User user = null;
			
			if((user = (User) database.load(User.class, request.parameters.get("username"))) != null) {
				if(user.isActivated()) {
					sendRecoverMail(user, request);
					return responder.redirect("/recover/confirm");
				} else {
					messages.add("user", "not-activated");
				}
			} else {
				messages.add("user", "does-not-exist");
			}
			
			request.session.addFlash("errors", messages);
			return responder.redirect("/recover");
		});
		
		server.on("GET", "/recover/confirm", (Request request) -> {
			return responder.render("recover/confirm.html", request.languages);
		});
		
		server.on("GET", "/unlock", (Request request) -> {	
			User user = null;
			if((user = (User) database.loadId(User.class, request.parameters.get("id"))) != null) {
				if(user.getKey().equals(request.parameters.get("key"))) {
					request.session.login(user.getUsername());
					return responder.redirect("/profile/password");
				}
			}
			return responder.render("recover/unlock.html", request.languages);
		});
		
	}
	
	private void profileRoutes() {
		
		server.on("ALL", "/profile.*", (Request request) -> {
			if(request.session.getUsername() == null) {
				return responder.redirect("/signin");
			} else {
				return responder.next();
			}
		});
		
		server.on("GET", "/profile", (Request request) -> {
			User user = null;
			if((user = (User) database.load(User.class, request.session.getUsername())) != null) {
				HashMap <String, Object> variables = new HashMap <String, Object> ();
				variables.put("activated", user.isActivated());
				variables.put("admin", user.isAdmin());
				return responder.render("profile/index.html", request.languages, variables);
			}
			return responder.redirect("/signin");
		});
		
		server.on("GET", "/profile/email", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			Messages messages = (Messages) request.session.getFlash("errors");
			if(messages != null) {
				messages.addToVariables(variables, "errors");
			}
			return responder.render("profile/email.html", request.languages, variables);
		});
		
		server.on("POST", "/profile/email", (Request request) -> {
			Messages messages = new Messages();
			User user = null;
			if((user = (User) database.load(User.class, request.session.getUsername())) != null) {
				user.setMail(request.parameters.get("email"));
				user.setActivated(false);
				if(user.validate(messages)) {
					if(database.update(user)) {
						sendActivationMail(user, request);
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
			
			return responder.render("profile/password.html", request.languages, variables);
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
			
			return responder.render("profile/delete.html", request.languages, variables);
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
		
	}

	private void sendActivationMail(User user, Request request) {
		HashMap <String, Object> variables = new HashMap <String, Object> ();
		variables.put("username", user.getUsername());
		variables.put("encrypted-username", Database.encrypt(user.getUsername()));
		variables.put("key", user.getKey());
		mailer.send(user.getMail(), "{{print translate \"activate-account\"}}", "activate.html", request.languages, variables);
	}
	
	private void sendRecoverMail(User user, Request request) {
		HashMap <String, Object> variables = new HashMap <String, Object> ();
		variables.put("username", user.getUsername());
		variables.put("encrypted-username", Database.encrypt(user.getUsername()));
		variables.put("key", user.getKey());
		mailer.send(user.getMail(), "{{print translate \"recover-account\"}}", "recover.html", request.languages, variables);
	}
	
}
