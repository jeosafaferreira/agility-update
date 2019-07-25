package Agility.outros;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import javax.swing.JOptionPane;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jferreira
 */
public class AgilityUpdate {

    private static final String SERVER_ADDR = "http://192.168.1.15";

    public static void saveError(String cod, Exception e) {
        JOptionPane.showMessageDialog(null, "Lembrar de salvar este erro no server\n" + e);
        System.out.println(e);
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            //Lê conteúdo da URL
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }

            String jsonText = sb.toString();
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    public static String getServerAddr() {
        return SERVER_ADDR;
    }
}
