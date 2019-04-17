package application;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import database.Database;
import database.templates.ObjectTemplate;
import database.validator.Validator;
import mailer.Mailer;
import manager.DatabaseSessionManager;
import responder.RenderResponder;
import server.Request;
import server.Server;

public class Application {
	
	private static final boolean PRODUCTION = true;
	
	private Database database;
	private RenderResponder responder;
	private Mailer mailer;
	private Server server;
	
	private HashMap <String, Object> predefined = new HashMap <String, Object>();

	
	public Application() throws IOException {		
		database = new Database();
		responder = new RenderResponder(predefined, new File("views/web"));
		mailer = new Mailer(predefined, new File("views/mail"));
		server = new Server(8000, new File("public"), responder, new DatabaseSessionManager <User> (database, 7 * 24 * 60 * 60, User::new));
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
			User user = (User) request.session.load();
			if(user != null) {
				predefined.put("username", user.getUsername());
			} else {
				predefined.put("username", null);
			}
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
			
			User user = (User) request.session.load();
			if(user != null) {
				variables.put("admin", user.isAdmin());
				admin = user.isAdmin();
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
			Integer next = (articleObjects != null && articleObjects.size() > (page + 1) * range) ? (page + 1) : null;
			
			variables.put("previous", previous);
			variables.put("next", next);
						
			variables.put("articles", articles);
			
			return responder.render("articles/index.html", request.languages, variables);
		});
		
		server.on("GET", "/articles/create", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			addMessagesFlashToVariables(request, "errors", variables);
			return responder.render("articles/create.html", request.languages, variables);
		});
		
		server.on("POST", "/articles/create", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {
				Article article = new Article();
				article.parseFromParameters(request.parameters);
				article.setAuthor(user);

				Validator validator = new Validator("errors");
				
				if(article.validate(validator)) {
					if(database.save(article)) {
						return responder.redirect("/articles");
					}
				}

				request.session.addFlash(validator);
				return responder.redirect("/articles/create");
			}
			return responder.redirect("/signin");
		});
		
		server.on("GET", "/articles/article/(.*)", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();

			boolean admin = false;
			
			User user = (User) request.session.load();
			if(user != null) {				
				variables.put("admin", user.isAdmin());
				admin = user.isAdmin();
			}
			
			final Article article;
			if((article = (Article) database.loadId(Article.class, request.groups.get(0))) != null) {
				if(!article.isVisible() && !admin) {
					return responder.redirect("/signin");
				}
				variables.put("article", article.getValues());
				addMessagesFlashToVariables(request, "errors", variables);

				int page = 0;
				int range = 8;
				if(request.parameters.containsKey("page")) {
					page = Integer.parseInt(request.parameters.get("page"));
				}

				LinkedList <ObjectTemplate> commentObjects = null;
				commentObjects = database.loadAll(Comment.class, (ObjectTemplate objectTemplate) -> {
					Comment comment = (Comment) objectTemplate;
					return comment.getParent().equals(article);
				});
				LinkedList <HashMap <String, Object>> comments = new LinkedList <HashMap <String, Object>> ();
				
				if(commentObjects != null) {
					for(int i = 0; i < commentObjects.size(); i++) {
						if(commentObjects.size() - 1 - i >= page * range && commentObjects.size() - 1 - i < (page + 1) * range) {
							Comment comment = (Comment) commentObjects.get(i);
							comments.addFirst(comment.getValues());
							comments.get(0).put("deletable", (comment.getAuthor().equals(user) || admin));
						}
					}
				}

				Integer previous = (page > 0) ? (page - 1) : null;
				Integer next = (commentObjects != null && commentObjects.size() > (page + 1) * range) ? (page + 1) : null;
				
				variables.put("previous", previous);
				variables.put("next", next);
				variables.put("comments", comments);
				
				return responder.render("articles/article.html", request.languages, variables);
			} else {
				return responder.redirect("/articles");
			}
			
		});
		
		
		server.on("POST", "/articles/article/(.*)", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {				
				Article article = null;
				if((article = (Article) database.loadId(Article.class, request.groups.get(0))) != null) {
					if(article.isVisible() || user.isAdmin()) {
						Comment comment = new Comment();
						comment.parseFromParameters(request.parameters);
						comment.setAuthor(user);
						comment.setParent(article);
						Validator validator = new Validator("errors");
						
						if(comment.validate(validator)) {
							if(database.save(comment)) {
								return responder.redirect("/articles/article/" + request.groups.get(0));
							}
						}

						request.session.addFlash(validator);
						
						return responder.redirect("/articles/article/" + request.groups.get(0));
					}
				}
				return responder.redirect("/articles");
			}
			return responder.redirect("/signin");
			
		});
		
		server.on("GET", "/articles/delete/(.*)", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {				
				if(user.isAdmin()) {
					if(database.deleteId(Article.class, request.groups.get(0))) {
						return responder.redirect("/articles");
					}
					return responder.redirect("/articles/article/" + request.groups.get(0));
				}
			}
			
			return responder.redirect("/signin");
			
		});
		
		server.on("GET", "/articles/visible/(.*)", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {				
				if(user.isAdmin()) {
					Article article = null;
					if((article = (Article) database.loadId(Article.class, request.groups.get(0))) != null) {
						boolean visible = article.isVisible();
						article.setVisible(!visible);
						if(!visible) {
							sendPublishedMail(article, request);
						}
						database.update(article);
					}
				}
			}
			
			return responder.redirect("/articles/article/" + request.groups.get(0));
		});
		
		server.on("GET", "/articles/edit/(.*)", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {				
				if(user.isAdmin()) {
					Article article = null;
					if((article = (Article) database.loadId(Article.class, request.groups.get(0))) != null) {
						HashMap <String, Object> variables = new HashMap <String, Object> ();
						variables.put("article", article.getValues());
						addMessagesFlashToVariables(request, "errors", variables);
						return responder.render("articles/edit.html", request.languages, variables);
					}
					return responder.redirect("/articles");
				}
			}
			
			return responder.redirect("/signin");
		});
		
		server.on("POST", "/articles/edit/(.*)", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {
				if(user.isAdmin()) {
					Article article = null;
					if((article = (Article) database.loadId(Article.class, request.groups.get(0))) != null) {
						article.parseFromParameters(request.parameters);
						article.setAuthor(user);
						Validator validator = new Validator("errors");
						if(article.validate(validator)) {
							if(database.update(article)) {
								return responder.redirect("/articles/article/" + request.groups.get(0));
							}
						}
						
						request.session.addFlash(validator);
						return responder.redirect("/articles/edit/" + request.groups.get(0));
					}
					return responder.redirect("/articles");
				}
			}
			return responder.redirect("/signin");
		});
	
		server.on("GET", "/comments/delete/(.*)", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {
				Comment comment = null;
				if((comment = (Comment) database.loadId(Comment.class, request.groups.get(0))) != null) {
					Article article = comment.getParent();
					if(user.isAdmin() || comment.getAuthor().equals(user)) {
						database.deleteId(Comment.class, request.groups.get(0));
					}
					return responder.redirect("/articles/article/" + article.getId());
				}
			}
			
			return responder.redirect("/signin");
			
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
			addMessagesFlashToVariables(request, "errors", variables);
			return responder.render("signin.html", request.languages, variables);
		});
		
		server.on("POST", "/signin", (Request request) -> {
			Validator validator = new Validator("errors");
			User user = (User) request.session.load();
			if(user != null) {
				if(user.authenticate(request.parameters.get("password"))) {
					request.session.save(user);
					return responder.redirect("/");
				} else {
					validator.addMessage("password", "does-not-match");
				}
			} else {
				validator.addMessage("user", "does-not-exist");
			}
			
			request.session.addFlash(validator);
			return responder.redirect("/signin");
		});
		
		server.on("GET", "/signup", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			addMessagesFlashToVariables(request, "errors", variables);
			return responder.render("signup.html", request.languages, variables);
		});
		
		server.on("POST", "/signup", (Request request) -> {
			User user = new User();
			user.parseFromParameters(request.parameters);

			Validator validator = new Validator("errors");
			
			if(user.validate(validator)) {
				if(database.save(user)) {
					request.session.save(user);
					
					sendActivationMail(user, request);
					
					return responder.redirect("/");
				} else {
					validator.addMessage("username", "in-use");
				}
			}

			request.session.addFlash(validator);
			return responder.redirect("/signup");
		});
		
		server.on("GET", "/signout", (Request request) -> {
			request.session.delete();
			return responder.redirect("/");
		});
		
	}
	
	private void recoveryRoutes() {
		
		server.on("GET", "/activate", (Request request) -> {	
			User user = null;
			if((user = (User) database.loadId(User.class, request.parameters.get("id"))) != null) {
				if(user.keyEquals(request.parameters.get("key"))) {
					request.session.save(user);
					user.setActivated(true);
					database.update(user);
					return responder.redirect("/profile");
				}
			}
			return responder.render("recover/activate.html", request.languages);
		});
		
		server.on("GET", "/recover", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			addMessagesFlashToVariables(request, "errors", variables);
			return responder.render("recover/index.html", request.languages, variables);
		});
		
		server.on("POST", "/recover", (Request request) -> {
			Validator validator = new Validator("errors");
			User user = null;
			
			if((user = (User) database.load(User.class, request.parameters.get("username"))) != null) {
				if(user.isActivated()) {
					sendRecoverMail(user, request);
					return responder.redirect("/recover/confirm");
				} else {
					validator.addMessage("user", "not-activated");
				}
			} else {
				validator.addMessage("user", "does-not-exist");
			}
			
			request.session.addFlash(validator);
			return responder.redirect("/recover");
		});
		
		server.on("GET", "/recover/confirm", (Request request) -> {
			return responder.render("recover/confirm.html", request.languages);
		});
		
		server.on("GET", "/unlock", (Request request) -> {	
			User user = null;
			if((user = (User) database.loadId(User.class, request.parameters.get("id"))) != null) {
				if(user.keyEquals(request.parameters.get("key"))) {
					request.session.save(user);
					return responder.redirect("/profile/password");
				}
			}
			return responder.render("recover/unlock.html", request.languages);
		});
		
	}
	
	private void profileRoutes() {
		
		server.on("GET", "/profile", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {
				HashMap <String, Object> variables = new HashMap <String, Object> ();
				variables.put("activated", user.isActivated());
				variables.put("admin", user.isAdmin());
				variables.put("notifications", user.notificationsEnabled());
				return responder.render("profile/index.html", request.languages, variables);
			}
			return responder.redirect("/signin");
		});
		
		server.on("GET", "/profile/email", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			addMessagesFlashToVariables(request, "errors", variables);
			return responder.render("profile/email.html", request.languages, variables);
		});
		
		server.on("POST", "/profile/email", (Request request) -> {
			Validator validator = new Validator("errors");
			User user = (User) request.session.load();
			if(user != null) {
				user.setMail(request.parameters.get("email"));
				user.setActivated(false);
				if(user.validate(validator)) {
					if(database.update(user)) {
						sendActivationMail(user, request);
						return responder.redirect("/profile");
					}
				}
			} else {
				validator.addMessage("user", "does-not-exist");
			}
			
			request.session.addFlash(validator);
			return responder.redirect("/profile/email");
		});
		
		server.on("GET", "/profile/password", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			addMessagesFlashToVariables(request, "errors", variables);
			return responder.render("profile/password.html", request.languages, variables);
		});
		
		server.on("POST", "/profile/password", (Request request) -> {
			Validator validator = new Validator("errors");
			User user = (User) request.session.load();
			if(user != null) {
				user.setPassword(request.parameters.get("password"));
				if(user.validate(validator)) {
					if(database.update(user)) {
						return responder.redirect("/profile");
					}
				}
			} else {
				validator.addMessage("user", "does-not-exist");
			}
			
			request.session.addFlash(validator);
			return responder.redirect("/profile/password");
		});
		
		server.on("GET", "/profile/notifications", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {
				user.toggleNotifications();
				if(user.validate()) {
					database.update(user);
				}
			} else {
				return responder.redirect("/signin");
			}
			
			return responder.redirect("/profile");
		});
		
		server.on("GET", "/profile/delete", (Request request) -> {
			HashMap <String, Object> variables = new HashMap <String, Object> ();
			addMessagesFlashToVariables(request, "errors", variables);
			return responder.render("profile/delete.html", request.languages, variables);
		});
		
		server.on("GET", "/profile/delete/confirm", (Request request) -> {
			User user = (User) request.session.load();
			if(user != null) {
				database.deleteAll(Article.class, (ObjectTemplate article) -> {
					return ((Article) article).getAuthor().equals(user);
				});
				database.deleteAll(Comment.class, (ObjectTemplate article) -> {
					return ((Comment) article).getAuthor().equals(user);
				});
				if(database.delete(User.class, ((String) request.session.load()))) {
					request.session.delete();
					return responder.redirect("/");
				}
			}
			Validator validator = new Validator("errors");
			validator.addMessage("user", "deletion-error");
			request.session.addFlash(validator);
			return responder.redirect("/profile/delete");
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
	
	private void sendPublishedMail(Article article, Request request) {
		HashMap <String, Object> variables = new HashMap <String, Object> ();
		variables.put("author", article.getAuthor().getUsername());
		variables.put("headline", article.getHeadline());
		variables.put("lead", article.getLead());
		variables.put("id", article.getId());
		LinkedList <ObjectTemplate> users = database.loadAll(User.class, (ObjectTemplate objectTemplate) -> {
			return ((User) objectTemplate).isActivated() && ((User) objectTemplate).notificationsEnabled();
		});
		for(ObjectTemplate user : users) {
			variables.put("username", ((User) user).getUsername());
			mailer.send(((User) user).getMail(), "{{print translate \"article-published\"}}", "published.html", request.languages, variables);
		}
	}
	
	private void addMessagesFlashToVariables(Request request, String name, HashMap <String, Object> variables) {
		Validator validator = (Validator) request.session.getFlash(name);
		if(validator != null) {
			validator.addToVariables(variables);
		}
	}
	
}
