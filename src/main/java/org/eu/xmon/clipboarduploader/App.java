package org.eu.xmon.clipboarduploader;

import okhttp3.*;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

public class App {
    private static TrayIcon trayIcon = new TrayIcon(new ImageIcon(Objects.requireNonNull(App.class.getResource("/logo.png"))).getImage());
    /* SystemTray */
    private static SystemTray tray = SystemTray.getSystemTray();
    /* PopupMenu */
    private static final MenuItem upload$item = new MenuItem("Upload image");
    private static final MenuItem exit$item = new MenuItem("Exit");
    private static final PopupMenu popup = new PopupMenu();
    private static final OkHttpClient client = new OkHttpClient();
    private static String user;
    private static String token;
    public static void main(String[] args) {
        if(!SystemTray.isSupported()){
            JOptionPane.showMessageDialog(null, "Twój system nie wspiera SystemTray'a", "xmonClipBoardUploader", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
        if (!new File("config.properties").exists()) {
            try (OutputStream output = new FileOutputStream("config.properties")) {
                Properties prop = new Properties();
                prop.setProperty("user", "user-uuid");
                prop.setProperty("token", "token-uuid");
                prop.store(output, "by Xmon");
                System.exit(-1);
            } catch (IOException io) {
                io.printStackTrace();
            }
        }else{
            try (InputStream input = new FileInputStream("config.properties")) {

                Properties prop = new Properties();

                // load a properties file
                prop.load(input);
                user = prop.getProperty("user");
                token = prop.getProperty("token");

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        popup.add("xmonClibBoardUploader");
        popup.getItem(0).disable();
        popup.addSeparator();
        popup.add(upload$item);
        popup.add(exit$item);
        exit$item.addActionListener((actionEvent) ->{
            System.exit(-1);
        });
        upload$item.addActionListener((actionEvent) ->{
            Image image = getImageFromClipboard();
            if (image != null){
                BufferedImage bufferedImage = toBufferedImage(image);
                try {
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("user", user)
                            .addFormDataPart("token", token)
                            .addFormDataPart("file", UUID.randomUUID() + ".png",
                                    RequestBody.create(
                                            toByteArray(bufferedImage, "png"),
                                            MediaType.get("image/png")))
                            .build();
                    Request request = new Request.Builder()
                            .url("https://i.xmon.eu.org/api/upload")
                            .post(requestBody)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                        StringSelection selection = new StringSelection(new JSONObject(response.body().string()).getString("url"));
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                        response.close();
                    }
                    requestBody = null;
                    request = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bufferedImage = null;
            }
            image = null;

        });
        trayIcon.setPopupMenu(popup);
        trayIcon.setToolTip("xmonClibBoardUploader");
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
    public static byte[] toByteArray(BufferedImage bi, String format)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, format, baos);
        byte[] bytes = baos.toByteArray();
        return bytes;

    }
    public static Image getImageFromClipboard()
    {
        Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor))
        {
            try
            {
                return (Image) transferable.getTransferData(DataFlavor.imageFlavor);
            }
            catch (UnsupportedFlavorException | IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            JOptionPane.showMessageDialog(null, "Niepoprawne zdjęcie w clipboardzie..", "xmonClipBoardUploader", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new
                BufferedImage(img.getWidth(null), img.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }
}
