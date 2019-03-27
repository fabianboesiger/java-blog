package application;

import java.util.Calendar;
import java.util.HashMap;

import database.templates.BooleanTemplate;
import database.templates.ObjectTemplate;
import database.templates.ObjectTemplateReference;
import database.templates.StringTemplate;

public class Article extends ObjectTemplate {
	
	public static final String NAME = "articles";
		
	private StringTemplate headline;
	private StringTemplate lead;
	private StringTemplate content;
	private ObjectTemplateReference <User> author;
	private BooleanTemplate visible;
	
	public Article() {
		headline = new StringTemplate("headline", 0, 128);
		lead = new StringTemplate("lead", 0, 1024);
		content = new StringTemplate("content", 0, 8192);
		author = new ObjectTemplateReference <User> ("author", User::new);
		visible = new BooleanTemplate("visible");
		visible.set(false);
	}

	public void setAuthor(User user) {
		author.set(user);
	}
	
	public User getAuthor() {
		return (User) author.get();
	}
	
	public String getLead() {
		return (String) lead.get();
	}
	
	public String getHeadline() {
		return (String) headline.get();
	}
	
	public HashMap <String, Object> getValues(){
		HashMap <String, Object> map = new HashMap <String, Object> ();
		map.put("headline",	headline.get());
		map.put("lead",	lead.get());
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
		map.put("visible", visible.get());
		return map;
	}
	
	public void setVisible(boolean value) {
		visible.set(value);
	}
	
	public boolean isVisible() {
		return (Boolean) visible.get();
	}
}
