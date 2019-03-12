package application;

import database.templates.ObjectTemplate;
import database.templates.StringTemplate;

public class Article extends ObjectTemplate {
		
	private StringTemplate title;
	
	public Article() {
		this(null);
	}
	
	public Article(String name) {
		super(name);
		
		title = new StringTemplate("title", 1, 128);
	}

}
