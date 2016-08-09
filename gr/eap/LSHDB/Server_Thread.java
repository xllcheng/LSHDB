/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.eap.LSHDB;

/**
 *
 * @author dimkar Each query received by Server_RDS is allocated to a Thread of 
 */
import gr.eap.LSHDB.util.QueryRecord;
import gr.eap.LSHDB.util.Result;
import gr.eap.LSHDB.util.URLUtil;
import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;

public class Server_Thread extends Thread {

    public static String GET_INDEXED_FIELDS = "GET INDEXED FIELDS FROM _";
    public static String GET_INDEXED_DATABASES = "GET INDEXED DATABASES";
    private Socket socket = null;
    private QueryRecord query;
    private DataStore[] lsh;
    private Result result;

    public Server_Thread(String name, Socket socket, DataStore[] lsh) {        
        this.socket = socket;
        System.out.println("Connection from: " + this.socket.getInetAddress().getHostAddress());
        this.lsh = lsh;
    }

    public Result getResult() {
        return this.result;
    }

    public DataStore getDB(String dbName) {
        for (int i = 0; i < lsh.length; i++) {
            if (lsh[i].getDbName().equals(dbName)) {
                return lsh[i];
            }
        }
        return null;
    }

   

    public void sendMsgAsObject(Object response) throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.writeObject(response);
        outputStream.flush();
        outputStream.close();
    }

    public void sendMsgAsStream(String response) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.write(response);
        writer.flush();
        writer.close();
    }

    public void run() {
        Object stream = null;
        try {
            ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
            stream = inStream.readObject();

        } catch (StreamCorruptedException ex) {

            try {
                InputStream inStream = socket.getInputStream();

                String s = "";
                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, Charset.forName(StandardCharsets.UTF_8.name())));
                s = reader.readLine();
                stream = s;
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }

        } catch (IOException e) {
            System.out.println("IOException Raised.");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException Raised.");
            e.printStackTrace();
        }

        try {

            if (stream instanceof QueryRecord) {
                this.query = (QueryRecord) stream;
                String dbName = this.query.getDbName();                
                DataStore db = getDB(dbName);
                
                result = new Result(this.query);
                if (db == null) {
                    result.setStatus(Result.STATUS_DATABASE_NOT_FOUND);
                    result.setMsg("The database " + dbName + " does not exist.");
                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                    outputStream.writeObject(result);
                    socket.close();
                    return;
                }
                
                
                result.setStatus(Result.STATUS_OK);
                long tStartInd = System.nanoTime();                                
                result = db.query(query);
                long tEndInd = System.nanoTime();
                long elapsedTimeInd = tEndInd - tStartInd;
                double secondsInd = elapsedTimeInd / 1.0E09;
                result.setTime(secondsInd);
                System.out.println("Query completed in " + secondsInd + " secs.");
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                result.prepare();
                outputStream.writeObject(result);
                outputStream.close();

            }

            if (stream instanceof String) {
                String msg = (String) stream;
                StringBuffer response = new StringBuffer("");

                if (msg.startsWith(JSON.JSON_REQUEST)) {
                    JSON json = new JSON();
                    String dbName = json.getDbName(msg);
                    String s = json.convert(msg, 20, getDB(dbName));
                    sendMsgAsStream(s);
                    socket.close();
                    return;

                }

                if (msg.equals("SHOW STORES")) {
                    for (int i = 0; i < lsh.length; i++) {
                        response.append(lsh[i].getDbName());
                        response.append(" meterialized by ");
                        response.append(lsh[i].getDbEngine());
                        response.append("\\n");
                    }
                    sendMsgAsObject(response);
                }

                if (msg.equals("GET INDEXED DATABASES")) {
                    Vector<String> dbs = new Vector<String>();
                    for (int i = 0; i < lsh.length; i++) {
                        if (lsh[i].getConfiguration().isKeyed()) {
                            dbs.add(lsh[i].getDbName());
                        }
                    }
                    sendMsgAsObject(dbs);
                }

                if (msg.startsWith(GET_INDEXED_FIELDS)) {
                    String dbName = msg.substring(msg.indexOf("_") + 1);
                    DataStore lsh = getDB(dbName);
                    sendMsgAsObject(lsh.getConfiguration().getKeyFieldNames());
                }

                if (msg.equals("SHOW STATS")) {
                    for (int i = 0; i < lsh.length; i++) {
                        response.append(lsh[i].getDbName() + "\r\n");
                        //response.append("keys=" + lsh[i].keys.count() + "\r\n");
                        response.append("records=" + lsh[i].records.count() + "\r\n");
                        response.append("==============================\r\n");
                    }
                    sendMsgAsObject(response);
                }

            }

            socket.close();

        } catch (IOException ex2) {
            System.out.println("IOException Raised.");
            ex2.printStackTrace();
        }

    }
}