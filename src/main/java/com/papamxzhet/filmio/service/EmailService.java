package com.papamxzhet.filmio.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.logo.url}")
    private String logoUrl;

    public void sendVerificationEmail(String to, String verificationCode) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Код подтверждения для filmio");

        String content = getEmailTemplate(
                "Подтверждение входа в filmio",
                "Для завершения процесса входа, пожалуйста, используйте следующий код подтверждения:",
                verificationCode,
                "Этот код действителен в течение 10 минут. Если вы не запрашивали этот код, пожалуйста, проигнорируйте это сообщение."
        );

        helper.setText(content, true);
        mailSender.send(message);
    }

    public void sendEmailVerification(String to, String verificationCode) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Подтверждение адреса электронной почты - filmio");

        String content = getEmailTemplate(
                "Подтверждение адреса электронной почты",
                "Благодарим за регистрацию в filmio! Для подтверждения вашего адреса электронной почты, пожалуйста, используйте следующий код:",
                verificationCode,
                "Этот код действителен в течение 10 минут."
        );

        helper.setText(content, true);
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String resetToken) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Сброс пароля - filmio");

        String resetUrl = "https://filmioapp.ru/reset-password?token=" + resetToken;

        String content = getPasswordResetTemplate(
                "Сброс пароля",
                "Вы запросили сброс пароля для своего аккаунта filmio. Нажмите на кнопку ниже, чтобы создать новый пароль:",
                resetUrl,
                "Эта ссылка действительна в течение 1 часа. Если вы не запрашивали сброс пароля, пожалуйста, проигнорируйте это сообщение."
        );

        helper.setText(content, true);
        mailSender.send(message);
    }

    private String getPasswordResetTemplate(String title, String preButtonText, String resetUrl, String postButtonText) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <link rel='preconnect' href='https://fonts.googleapis.com'>" +
                "    <link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>" +
                "    <link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap' rel='stylesheet'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: \"Inter\", Arial, sans-serif; background-color: #f8f9fa; color: #333333;'>" +
                "    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 12px 24px rgba(0,0,0,0.06); margin-top: 20px; margin-bottom: 20px;'>" +
                "        <div style='background: linear-gradient(135deg, #53007A 0%, #7e02b7 100%); padding: 40px 0; text-align: center;'>" +
                "            <div style='width: 90px; height: 90px; margin: 0 auto; background-color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 16px rgba(0,0,0,0.15);'>" +
                "                <img src='" + logoUrl + "' alt='filmio logo' style='width: 60px; height: 60px; object-fit: contain; object-position: center' />" +
                "            </div>" +
                "            <h1 style='color: #ffffff; font-size: 26px; margin-top: 20px; font-weight: 600; letter-spacing: 0.5px;'>filmio</h1>" +
                "        </div>" +
                "        <div style='padding: 40px 50px;'>" +
                "            <h2 style='color: #53007A; margin-top: 0; margin-bottom: 24px; font-weight: 600; font-size: 22px;'>" + title + "</h2>" +
                "            <p style='color: #555; font-size: 16px; line-height: 1.6; margin-bottom: 30px;'>" + preButtonText + "</p>" +
                "            <div style='text-align: center; margin: 40px 0;'>" +
                "                <a href='" + resetUrl + "' style='display: inline-block; padding: 15px 30px; background-color: #53007A; color: white; text-decoration: none; font-weight: 600; border-radius: 50px; font-size: 16px;'>Сбросить пароль</a>" +
                "            </div>" +
                "            <p style='color: #555; font-size: 16px; line-height: 1.6;'>" + postButtonText + "</p>" +
                "            <div style='margin: 40px 0 20px; padding: 20px; background-color: #f8f5fe; border-radius: 8px; border-left: 4px solid #53007A;'>" +
                "                <p style='margin: 0; color: #666; font-size: 14px;'>Если кнопка не работает, скопируйте и вставьте эту ссылку в браузер:</p>" +
                "                <p style='margin: 10px 0 0; word-break: break-all; color: #53007A; font-size: 14px;'>" + resetUrl + "</p>" +
                "            </div>" +
                "        </div>" +
                "        <div style='background-color: #f8f5fe; padding: 30px; text-align: center; border-top: 1px solid #f0e6fa;'>" +
                "            <p style='color: #53007A; font-size: 15px; font-weight: 500; margin: 0 0 5px 0;'>С уважением, Команда Filmio</p>" +
                "            <p style='color: #888; font-size: 14px; margin: 5px 0 20px;'>Спасибо, что выбрали нас!</p>" +
                "            <p style='color: #aaa; font-size: 13px; margin-top: 15px;'>© " + java.time.Year.now().getValue() + " filmio. Все права защищены.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    private String getEmailTemplate(String title, String preCodeText, String verificationCode, String postCodeText) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <link rel='preconnect' href='https://fonts.googleapis.com'>" +
                "    <link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>" +
                "    <link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap' rel='stylesheet'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: \"Inter\", Arial, sans-serif; background-color: #f8f9fa; color: #333333;'>" +
                "    <div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 12px 24px rgba(0,0,0,0.06); margin-top: 20px; margin-bottom: 20px;'>" +
                "        <div style='background: linear-gradient(135deg, #53007A 0%, #7e02b7 100%); padding: 40px 0; text-align: center;'>" +
                "            <div style='width: 90px; height: 90px; margin: 0 auto; background-color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 16px rgba(0,0,0,0.15);'>" +
                "                <img src='" + logoUrl + "' alt='filmio logo' style='width: 60px; height: 60px; object-fit: contain; object-position: center' />" +
                "            </div>" +
                "            <h1 style='color: #ffffff; font-size: 26px; margin-top: 20px; font-weight: 600; letter-spacing: 0.5px;'>filmio</h1>" +
                "        </div>" +
                "        <div style='padding: 40px 50px;'>" +
                "            <h2 style='color: #53007A; margin-top: 0; margin-bottom: 24px; font-weight: 600; font-size: 22px;'>" + title + "</h2>" +
                "            <p style='color: #555; font-size: 16px; line-height: 1.6; margin-bottom: 30px;'>" + preCodeText + "</p>" +
                "            <div style='background-color: #f8f5fe; border-left: 4px solid #53007A; border-radius: 8px; padding: 25px; margin: 30px 0; text-align: center;'>" +
                "                <h3 style='color: #53007A; font-size: 32px; letter-spacing: 8px; margin: 0; font-weight: 700;'>" + verificationCode + "</h3>" +
                "            </div>" +
                "            <p style='color: #555; font-size: 16px; line-height: 1.6;'>" + postCodeText + "</p>" +
                "            <div style='margin: 40px 0 20px; text-align: center;'>" +
                "                <a href='https://filmioapp.ru' style='display: inline-block; padding: 12px 24px; background-color: #53007A; color: white; text-decoration: none; font-weight: 500; border-radius: 50px; font-size: 16px;'>Перейти в filmio</div>" +
                "            </div>" +
                "        </div>" +
                "        <div style='background-color: #f8f5fe; padding: 30px; text-align: center; border-top: 1px solid #f0e6fa;'>" +
                "            <p style='color: #53007A; font-size: 15px; font-weight: 500; margin: 0 0 5px 0;'>С уважением, Команда Filmio</p>" +
                "            <p style='color: #888; font-size: 14px; margin: 5px 0 20px;'>Спасибо, что выбрали нас!</p>" +
                "            <p style='color: #aaa; font-size: 13px; margin-top: 15px;'>© " + java.time.Year.now().getValue() + " filmio. Все права защищены.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}