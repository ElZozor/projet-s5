package utils;

import launch.ClientLaunch;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class Utils {

    public static String getCurrentPath() {
        String path = "/";
        try {
            path = Paths.get(ClientLaunch.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();

            if (path.contains("/")) {
                path = path.substring(0, path.lastIndexOf('/')) + "/";
            } else {
                path = "/";
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return path;
    }


    public static String getPathOfFile(final String filename) {
        return getCurrentPath() + filename;
    }


    public static void saveToFile(final String filename, final String data) {
        final String currentPath = getCurrentPath() + "data/";
        new File(currentPath).mkdirs();

        File file = new File(currentPath + filename);
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(data.getBytes(), 0, data.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String loadFromFile(final String filename) {
        final String currentPath = getCurrentPath() + "data/";

        File file = new File(currentPath + filename);
        try (BufferedReader reader = new BufferedReader(new FileReader(file));) {
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

}
