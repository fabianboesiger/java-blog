package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import database.Database;
import database.templates.IdentifiableStringTemplate;
import database.templates.ObjectTemplate;
import database.templates.StringTemplate;

public class User extends ObjectTemplate {
	
	private static final File MAIL_DATA = new File("local/mail.txt");
		
	private IdentifiableStringTemplate username;
	private StringTemplate password;
	private StringTemplate email;
	
	public User() {
		this(null);
	}
	
	public User(String name) {
		super(name);
		username = new IdentifiableStringTemplate("username", 2, 16);
		password = new StringTemplate("password", 4, 32, (Object value) -> {
			return hash((String) value);
		});
		email = new StringTemplate("email", 0, 64);
		setIdentifier(username);
	}
	
	public boolean authenticate(String password) {
		if(password != null) {
			try {
				if(hash(password).equals(this.password.get())) {
					return true;
				}
			} catch(NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public String getUsername() {
		return (String) username.get();
	}
	
	public static String hash (String input) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		messageDigest.update(input.getBytes());
		byte[] bytes = messageDigest.digest();
		StringBuilder stringBuilder = new StringBuilder();
		for(int i = 0; i < bytes.length ; i++) {
			stringBuilder.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return stringBuilder.toString();
	}
	
	public void sendMail(String subject, String body) throws IOException {

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(MAIL_DATA), Database.ENCODING));			
		String username = bufferedReader.readLine();
		String password = bufferedReader.readLine();
		bufferedReader.close();
		
		String to = (String) email.get();
		
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "587");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", "587");

        Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
