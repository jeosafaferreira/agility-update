package Agility.dal;

import java.sql.*;
import javax.swing.JOptionPane;

/**
 *
 * @author Jeosafá
 */
public class ModuloConexao {

    public static Connection conector() {
        java.sql.Connection conexao = null;
        String driver = "com.mysql.jdbc.Driver";
        String url, user, password;
        
            url = "jdbc:mysql://127.0.0.1:3306/agility?useSSL=false";
            user = "root";
            password = "root";

//            url = "jdbc:mysql://187.79.218.217:3306/Agility?useSSL=false";
//            user = "Agility";
//            password = "x|JrnbWKiJkYoLeH8L2{";
        try {
            Class.forName(driver);
            conexao = DriverManager.getConnection(url, user, password);
            return conexao;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Desculpe, ocorreu um erro ao conectar ao banco de dados.\nContate nosso suporte ligando para: (xx) xxxx-xxxx, ou através do nosso email: suporte@Agility.com.br\nCódigo do Erro: #0001\nDetalhes do erro:\n" + e);
            System.exit(0);
            return null;

        }
    }

}
