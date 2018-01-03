package mailer.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;

@Component
@NoArgsConstructor
@Log4j
public class MailSender {
	@Value("${first.folder}")
	String firstFolder;
	@Value("${second.folder}")
	String secondFolder;
	@Value("${first.recipient}")
	String firstRecipient;
	@Value("${second.recipient}")
	String secondRecipient;
	@Value("${mail.username}")
	String username;
	@Value("${mail.password}")
	String password;
	@Value ("${smtp.server}")
	String smtpServer;
	@Value("${from}")
	String from;
	@PostConstruct
	void work() {
		preFlightChecks();
		
		while (true) {
			log.info("Попытка отправки сообщений...");
		sendMailToFirstAddress();
		sendMailToSecondAddress();
		try {
			log.info("Спать 10 мин...");
			Thread.sleep(600000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		}
	}

	private void preFlightChecks() {
		log.info("Получатель 1: "+firstRecipient);
		log.info("Получатель 2: "+secondRecipient);
		log.info("Каталог с файлами первого отчета: "+firstFolder);
		log.info("Каталог с файлами второго отчета: "+secondFolder);
		log.info("Почтовый сервер: "+smtpServer);
		log.info("Имя пользователя: "+username);
		log.info("Поле От: "+from);
		if (!Paths.get(firstFolder).toFile().exists()) {
			log.error("Указан неправильный каталог для первого отчета");
		}
		if (!Paths.get(secondFolder).toFile().exists()) {
			log.error("Указан неправильный каталог для второго отчета");
		}
		if (firstRecipient.isEmpty()) {
			log.error("Пустой получатель для первого отчета");
		}
		if (secondRecipient.isEmpty()) {
			log.error("Пустой получатель для второго отчета");
		}
	}

	private void deleteFile(String path) {
		File file = Paths.get(path).toFile();
		if (file.exists()) {
			file.delete();
			log.info("Файл "+ file.getName() + " удален...");
		}

	}

	private String getFile(String path) {
		Path pathToFiles = Paths.get(path);
		try {
			DirectoryStream<Path> stream = Files.newDirectoryStream(pathToFiles);
			Iterator<Path> iterator = stream.iterator();
			if (iterator.hasNext()) {
				Path pathToFile = iterator.next();
				if (pathToFile.toFile().exists()) {
					if (pathToFile.toFile().isFile()) {
						return pathToFile.toString();
					}
				}
			}
		} catch (IOException e) {
			log.error("Ошибка I/O", e);
		}
		return null;

	}

	private void sendMailToSecondAddress() {
		String PATH = secondFolder;
		String RECIPIENT = secondRecipient;
		String SUBJECT = "Отчет Ейская ЦРБ";
		
		String file = getFile(PATH);
		if (Objects.nonNull(file)) {
			log.info("Попытка отправки второго отчета...");
			boolean success = sendMail(file, RECIPIENT, SUBJECT);
			if (success) {
				log.info("Второй отчет отправлен успешно...");
				deleteFile(file);
			}
		}

	}

	private void sendMailToFirstAddress() {
		String PATH = firstFolder;
		String RECIPIENT = firstRecipient;
		String SUBJECT = "Отчет Ейская ЦРБ";
		
		String file = getFile(PATH);
		if (Objects.nonNull(file)) {
			log.info("Попытка отправки первого отчета...");
			boolean success = sendMail(file, RECIPIENT, SUBJECT);
			if (success) {
				log.info("Первый отчет отправлен успешно...");
				deleteFile(file);
			}
		}

	}

	private boolean sendMail(String filename, String recipient, String subject) {
		// Recipient's email ID needs to be mentioned.
		// Assuming you are sending email through relay.jangosmtp.net
		String host = smtpServer;

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "25");

		// Get the Session object.
		Session session = Session.getInstance(props, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			// Create a default MimeMessage object.
			Message message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));

			// Set Subject: header field
			message.setSubject(subject);

			// Create the message part
			BodyPart messageBodyPart = new MimeBodyPart();

			// Now set the actual message
			messageBodyPart.setText("");

			// Create a multipar message
			Multipart multipart = new MimeMultipart();

			// Set text message part
			multipart.addBodyPart(messageBodyPart);

			// Part two is attachment
			messageBodyPart = new MimeBodyPart();
			DataSource source = new FileDataSource(filename);
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(Paths.get(filename).getFileName().toString());
			multipart.addBodyPart(messageBodyPart);

			// Send the complete message parts
			message.setContent(multipart);

			// Send message
			Transport.send(message);

			System.out.println("Сообщение успешно отправлено....");
			return true;

		} catch (MessagingException e) {
			log.error("Возникла ошибка отправки сообщение", e);
		}
		return false;
	}

}
