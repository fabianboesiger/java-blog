package application;

import java.util.Calendar;
import java.util.HashMap;

import database.templates.ObjectTemplate;
import database.templates.ObjectTemplateReference;
import database.templates.StringTemplate;

public class Comment extends ObjectTemplate {
	
	public static final String NAME = "comments";
		
	private StringTemplate content;
	private ObjectTemplateReference <Article> parent;
	private ObjectTemplateReference <User> author;
	
	public Comment() {
		content = new StringTemplate("content", 0, 128);
		parent = new ObjectTemplateReference <Article> ("parent", Article::new);
		author = new ObjectTemplateReference <User> ("author", User::new);
	}
	
	public void setParent(Article article) {
		parent.set(article);
	}
	
	public Article getParent() {
		return (Article) parent.get();
	}
	
	public void setAuthor(User user) {
		author.set(user);
	}
	
	public User getAuthor() {
		return (User) author.get();
	}
	
	public HashMap <String, Object> getValues(){
		HashMap <String, Object> map = new HashMap <String, Object> ();
		map.put("content", content.get());
		String username = null;
		User user = ((User) author.get());
		if(user != null) {
			username = user.getUsername();
		}
		map.put("author", username);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis((Long) timestamp.get());
		map.put("date", 
			String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)) + "." + 
			String.format("%02d", (calendar.get(Calendar.MONTH) + 1)) + "." + 
			calendar.get(Calendar.YEAR) + " " + 
			String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)) + ":" + 
			String.format("%02d", calendar.get(Calendar.MINUTE)));
		map.put("id", id);
		return map;
	}
	
}
