package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebWatcher {

    private static final String URL = "https://helpx.adobe.com/sign/system-requirements.html"; // URL to monitor
    private static final String FILE_PATH = "last_ip_addresses.txt"; // Path to save the last IP addresses
    private static final long INTERVAL = 60000; // Check interval in milliseconds (e.g., 60000ms = 1 minute)
    private static final String EMAIL_FROM = "your_email@example.com";
    private static final String EMAIL_TO = "recipient_email@example.com";
    private static final String EMAIL_SUBJECT = "Web Watcher IP Address Change Notification";
    private static final String SMTP_SERVER = "smtp.example.com";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USER = "your_email@example.com";
    private static final String SMTP_PASS = "your_email_password";

    public static void main(String[] args) {
        Timer timer = new Timer();
        timer.schedule(new MonitorTask(), 0, INTERVAL);
    }

    static class MonitorTask extends TimerTask {
        @Override
        public void run() {
            try {
                Document doc = Jsoup.connect(URL).timeout(10000).get();
                String htmlContent = doc.html();

                List<String> currentIpAddresses = extractIpAddresses(htmlContent);
                Collections.sort(currentIpAddresses);

                List<String> lastIpAddresses = readIpAddresses(FILE_PATH);

                if (!currentIpAddresses.equals(lastIpAddresses)) {
                    System.out.println("Change detected at " + new Date());
                    sendEmail(EMAIL_SUBJECT, "IP addresses have changed:\n" + String.join("\n", currentIpAddresses));
                    saveIpAddresses(FILE_PATH, currentIpAddresses);
                } else {
                    System.out.println("No change detected at " + new Date());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private List<String> extractIpAddresses(String html) {
            List<String> ipAddresses = new ArrayList<>();
            Pattern pattern = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");
            Matcher matcher = pattern.matcher(html);
            while (matcher.find()) {
                ipAddresses.add(matcher.group());
            }
            return ipAddresses;
        }

        private void saveIpAddresses(String filePath, List<String> ipAddresses) throws IOException {
            Files.write(Paths.get(filePath), String.join("\n", ipAddresses).getBytes());
        }

        private List<String> readIpAddresses(String filePath) throws IOException {
            if (!Files.exists(Paths.get(filePath))) {
                return new ArrayList<>();
            }
            return Files.readAllLines(Paths.get(filePath));
        }

        private void sendEmail(String subject, String body) {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_SERVER);
            props.put("mail.smtp.port", SMTP_PORT);

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_FROM));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);
                System.out.println("Email sent successfully.");
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

