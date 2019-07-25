package Agility.telas;

import Agility.dal.ModuloConexao;
import Agility.outros.AgilityUpdate;
import java.awt.Cursor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONObject;

/**
 *
 * @author jferreira
 */
public class Home extends javax.swing.JFrame {

    Connection con;
    ResultSet rs;
    String cli_id;

    public Home() {
        initComponents();
        con = ModuloConexao.conector();
        getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        start();
    }

    private void start() {
        try {
            rs = con.prepareStatement("SELECT school_cod FROM school_data").executeQuery();
            rs.next();
            cli_id = rs.getString(1);
            JSONObject json = AgilityUpdate.readJsonFromUrl(AgilityUpdate.getServerAddr() + "/fun.php?method=2&cli=" + cli_id);

            int l = json.length();
            for (int i = 0; i < l; i++) {
                String op = json.getJSONObject(i + "").getString("op");
                System.out.println(op);
                switch (op) {
                    case "1":
                        download(json.getJSONObject(i + ""));
                        break;
                    case "2":
                        listFiles(json.getJSONObject(i + ""));
                        break;
                    case "3":
                        dump(json.getJSONObject(i + ""));
                        break;
                    case "4":
                        upQuery(json.getJSONObject(i + ""));
                        break;
                }
            }
            Thread th = new Thread() {
                public void run() {

                    try {
                        for (int i = 1; i < 1001; i++) {
                            sleep(5);  //sleep para a thread adormecer por 1 segundo.
                            pb.setValue(i);
                        }
                        Runtime.getRuntime().exec("java -jar Agility_-_Secretaria.jar");
                        System.exit(0);
                    } catch (Exception ex) {
                        AgilityUpdate.saveError("#11", ex);
                    }
                }
            };
            th.start();

        } catch (Exception e) {
            AgilityUpdate.saveError("#01", e);
        }
    }

    private void refreshStatus(JSONObject json, boolean status) {
        try {
            new URL(AgilityUpdate.getServerAddr() + "/fun.php?method=3&id=" + json.get("id") + "&q=" + status).openStream();
        } catch (Exception e) {
            AgilityUpdate.saveError("#05", e);
        }
    }

    private boolean download(JSONObject json) {
        System.out.println("Op: Download");
        boolean status = true;
        try {
            String[] files = json.get("command").toString().split(";");
            for (String fileName : files) {
                URL url = new URL(AgilityUpdate.getServerAddr() + "/agilityUpdate/" + fileName);
                File file = new File(fileName);
                FileUtils.copyURLToFile(url, file);
            }
        } catch (Exception e) {
            AgilityUpdate.saveError("#02", e);
            status = false;
        } finally {
            //RETORNA STATUS
            try {
                refreshStatus(json, status);

            } catch (Exception e) {
                AgilityUpdate.saveError("#03", e);
            }
        }
        System.out.println(status);
        return status;
    }

    private boolean listFiles(JSONObject json) {
        System.out.println("Op: listFiles");
        String output = "";
        boolean status = true;
        for (Object t : FileUtils.listFiles(new File("."), null, true).toArray()) {
            output += t.toString() + ";";
        }
        output += "PATH: " + System.getProperty("user.dir");
        output = output.replace(" ", "%20").replace("\\", "__");
        try {
            System.out.println(AgilityUpdate.getServerAddr() + "/fun.php?method=4&id=" + json.get("id") + "&o=" + output);

            new URL(AgilityUpdate.getServerAddr() + "/fun.php?method=4&id=" + json.get("id") + "&o=\"" + output + "\"").openStream();
        } catch (Exception e) {
            status = false;
            AgilityUpdate.saveError("#04", e);
        } finally {
            refreshStatus(json, status);
        }
        System.out.println(status);
        return status;
    }

    private boolean dump(JSONObject json) {
        System.out.println("Op: Dump");
        boolean status = true;
        String dump = "";
        //GERA DUMP
        try {
            InputStream i = Runtime.getRuntime().exec("mysqldump -u root -proot " + json.get("command")).getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(i);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                dump += line;
            }
        } catch (Exception e) {
            AgilityUpdate.saveError("#06", e);
            status = false;
        }

        //CRIA ARQUIVO
        File arq = new File("temp/cli_" + cli_id + ".sql");
        try {
            File temp = new File("temp");
            if (!temp.isDirectory()) {
                temp.mkdir();
            }
            FileUtils.writeStringToFile(arq, dump.replace(";", ";\n"), "UTF-8");
        } catch (Exception e) {
            AgilityUpdate.saveError("#07", e);
            status = false;
        }

        //UPLOAD no SERVER
        try {
            FTPClient ftp = new FTPClient();
            ftp.connect("files.000webhost.com");
            ftp.login("sistemaagility", "34848998a");
            ftp.enterLocalPassiveMode();
            InputStream input = new FileInputStream(arq);
            ftp.storeFile("/public_html/agilityUpdate/dumps/" + arq.getName(), input);
            ftp.logout();
            ftp.disconnect();
        } catch (Exception e) {
            AgilityUpdate.saveError("#08", e);
            status = false;
        }

        //LIMPA ARQUIVO E RETORNA STATUS
        try {
            FileUtils.cleanDirectory(new File("temp/"));
            refreshStatus(json, status);
        } catch (Exception e) {
            AgilityUpdate.saveError("#09", e);
        }
        System.out.println(status);
        return status;
    }

    private boolean upQuery(JSONObject json) {
        System.out.println("Op: myQuery");
        boolean status = true;
        try {
            PreparedStatement pst = con.prepareStatement(json.get("command").toString());
            int rows = pst.executeUpdate();
            if (pst.executeUpdate() > 0) {
                new URL(AgilityUpdate.getServerAddr() + "/fun.php?method=4&id=" + json.get("id") + "&o=" + rows + "_rows_affected.").openStream();
            }
        } catch (Exception e) {
            AgilityUpdate.saveError("#10", e);
            status = false;

        }
        refreshStatus(json, status);

        System.out.println(status);
        return status;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblLogo = new javax.swing.JLabel();
        jXBusyLabel2 = new org.jdesktop.swingx.JXBusyLabel();
        pb = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Sistema Agility - Atualização");
        setUndecorated(true);
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        lblLogo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Agility/icones/logo.png"))); // NOI18N

        jXBusyLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Agility/icones/gears.gif"))); // NOI18N

        pb.setMaximum(1000);

        jLabel1.setText("Atualizando sistema. Por favor, aguarde...");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(89, 89, 89)
                .addComponent(jXBusyLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addContainerGap(89, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(lblLogo)
                        .addGap(170, 170, 170))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(pb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(186, 186, 186))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(lblLogo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jXBusyLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(34, 34, 34)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Home.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Home.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Home.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Home.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Home().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private org.jdesktop.swingx.JXBusyLabel jXBusyLabel2;
    private javax.swing.JLabel lblLogo;
    private javax.swing.JProgressBar pb;
    // End of variables declaration//GEN-END:variables
}
