package application;

import java.util.Calendar;
import java.util.HashMap;

import database.templates.ObjectTemplate;
import database.templates.ObjectTemplateReference;
import database.templates.StringTemplate;

public class Article extends ObjectTemplate {
		
	private StringTemplate headline;
	private StringTemplate lead;
	private StringTemplate content;
	private ObjectTemplateReference <User> author;
	
	public Article() {
		headline = new StringTemplate("headline", 0, 128);
		lead = new StringTemplate("lead", 0, 128);
		content = new StringTemplate("content", 0, 128);
		author = new ObjectTemplateReference <User> ("author", User::new);
	}

	public void setAuthor(User user) {
		author.set(user);
	}
	
	public HashMap <String, Object> getValues(){
		HashMap <String, Object> map = new HashMap <String, Object> ();
		map.put("headline",	headline.get());
		map.put("lead",	lead.get());
		map.put("content", content.get());
		map.put("author", ((User) author.get()).getUsername());
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis((Long) timestamp.get());
		map.put("date", 
			String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)) + "." + 
			String.format("%02d", (calendar.get(Calendar.MONTH) + 1)) + "." + 
			calendar.get(Calendar.YEAR) + " " + 
			String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)) + ":" + 
			String.format("%02d", calendar.get(Calendar.MINUTE)));
		return map;
	}

}
