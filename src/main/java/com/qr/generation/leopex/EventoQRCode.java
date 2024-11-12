package com.qr.generation.leopex;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
public class EventoQRCode {
    public static void main(String[] args) {
        String csvFilePath = "asistentes.csv";
        String qrDirectory = "qrs/";

        List<Asistente> asistentes = leerCSV(csvFilePath);

        for (Asistente asistente : asistentes) {
            if (asistente.getAsistencia().equalsIgnoreCase("Confirmado")) {
                String qrContent = "Nombre: " + asistente.getNombre() + "\nCorreo: " + asistente.getCorreo();
                String qrPath = qrDirectory + asistente.getNombre().replace(" ", "_") + ".png";

                try {
                    generarQRCode(qrContent, qrPath);
                    enviarCorreo(asistente.getCorreo(), "Entrada al Evento",
                            "Hola " + asistente.getNombre() + ", adjunto tu código QR para el evento.", qrPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<Asistente> leerCSV(String filePath) {
        List<Asistente> asistentes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Omitir encabezado
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                asistentes.add(new Asistente(values[0], values[1], values[2]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return asistentes;
    }

    public static void generarQRCode(String text, String filePath) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300);
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }

    public static void enviarCorreo(String to, String subject, String messageText, String filePath) {
        final String fromEmail = "tucorreo@gmail.com";
        final String password = "tu_contraseña";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(messageText);

            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource source = new FileDataSource(filePath);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(new File(filePath).getName());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);

            Transport.send(message);
            System.out.println("Correo enviado a: " + to);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}

class Asistente {
    private String nombre;
    private String correo;
    private String asistencia;

    public Asistente(String nombre, String correo, String asistencia) {
        this.nombre = nombre;
        this.correo = correo;
        this.asistencia = asistencia;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getAsistencia() {
        return asistencia;
    }
}
