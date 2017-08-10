/************************************************************************
 * Source filename: Notification.java
 * <p/>
 * Creation date: Sep 3, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.log4j.Logger;
import wde.data.Email;

public class Notification {

    private static final String defaultEmail = "wxde_support@leidoshost.net";
    public static boolean isStandAlone = false;
    public static boolean useGmail = false;
    private static Logger logger = Logger.getLogger(Notification.class);
    private static Notification instance = null;
    private Config config = null;
    private Properties props = null;

    private Notification() {
        props = new Properties();
        String mailServerStr = null;

        if (isStandAlone)
            useGmail = true;
        else {
            config = ConfigSvc.getInstance().getConfig(Notification.this);
            mailServerStr = config.getString("mail.smtp.host", null);
            if (mailServerStr.contains("gmail.com"))
                useGmail = true;
        }
        if (useGmail) {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else
            props.put("mail.smtp.host", mailServerStr);
    }

    public static Notification getInstance() {
        if (instance == null)
            instance = new Notification();

        return instance;
    }

    public static void send(Email email) {

        ArrayList<InternetAddress> emailAddresses = new ArrayList<InternetAddress>();
        try {
            emailAddresses.add(new InternetAddress(email.getTo()));
            Notification notification = Notification.getInstance();
            notification.sendEmail(emailAddresses, email.getSubject(), email.getBody(), null, null);
        } catch (AddressException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        Notification.isStandAlone = true;

        Email email = new Email();
        email.setBody("a test message");
        email.setSubject(" a test subject");
        email.setTo("ScotDLange@yahoo.com");Notification.send(email);

    }

    public void sendEmail(ArrayList<InternetAddress> recipients, String subject, String messageBody, String filePath, String fileDisplayName) {
        if (recipients.isEmpty()) {
            logger.info("sendEmail invoked with empty recipients");
            return;
        }

        Session session = null;

        if (useGmail)
            session = Session.getInstance(props, new Authenticator() {
                final String strUsername = "weatherdataenvironment@gmail.com";
                final String strPassword = "W3@ther1";

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(strUsername, strPassword);
                }
            });
        else
            session = Session.getInstance(props);

        try {

            // send one message when status changes to unacceptable
            // and a second message when normal operation resumes
            MimeMessage message = new MimeMessage(session);
            message.setSentDate(new Date());

            for (int nIndex = 0; nIndex < recipients.size(); nIndex++)
                message.setRecipient(Message.RecipientType.TO, recipients.get(nIndex));

            if (!useGmail) {
                String str = defaultEmail;
                str = config.getString("bcc", null);
                logger.info("bcc: " + str);

                message.setRecipient(Message.RecipientType.BCC, new InternetAddress(str));

                str = config.getString("mail.from", null);
                logger.info("from: " + str);

                message.setFrom(new InternetAddress(str));
            }

            // create and fill the first message part
            MimeBodyPart msgText = new MimeBodyPart();
            msgText.setText(messageBody);

            // create the Multipart message and add the parts to it
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(msgText);

            // create the second message part
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    MimeBodyPart msgFile = new MimeBodyPart();
                    msgFile.attachFile(filePath);
                    msgFile.setFileName(fileDisplayName);
                    multipart.addBodyPart(msgFile);
                }
            }

            // add the Multipart to the message
            message.setContent(multipart);

            message.setSubject(subject);

            Transport.send(message); // very last step

            if (useGmail)
                logger.info("Mail sent to: " + recipients.get(0).getAddress());
        } catch (MessagingException me) {
            logger.error(me.getMessage());
            me.printStackTrace();
        } catch (IOException ie) {
            logger.error(ie.getMessage());
            ie.printStackTrace();
        }
    }
}
