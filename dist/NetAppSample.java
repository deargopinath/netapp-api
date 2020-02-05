import java.util.*;
import java.io.*;




/**
 *
 * @author Srinivas Gopinath Parimi
 */
public class NetAppSample {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
      NetAppFiler filer = new NetAppFiler("192.168.65.130", "user", "password");
        
        HashMap<String, String> opts = filer.getOptions();
        for (String s : opts.keySet()) {
            System.out.println(s + " = " + opts.get(s));
        }
        System.out.println("Free space in aggregate aggr01 = " + filer.aggrFreeSpace("aggr01"));
        
        System.out.println(filer.jsh("ps -e"));
        
        System.out.println("Root aggregate = " + filer.getRootAggregate());
        System.out.println("Root volume = " + filer.getRootVolume());
        
        for (String s : filer.getAllFlexibleVolumes()) {
            System.out.println("Flexible volume = " + s);
        }
        
        for (String s : filer.getAllTraditionalVolumes()) {
            System.out.println("Traditional volume = " + s);
        }
        
        
      List<String> vols = new ArrayList<String>();
        vols.add("sms_v2_415");
        vols.add("sms_v3_415");
        String ipAddr = "192.168.1.2";
               
       for (String v : vols) {
           String cmd =  ("exportfs -c " + ipAddr + " /vol/" + v + "; ");
           System.out.println("Command = \n" + cmd);
           System.out.println("Result = \n" + filer.jsh(cmd));
       }
        

       
 
       BufferedReader publicKeyFile = new BufferedReader(
                          new FileReader("/home/gopinath/.ssh/id_rsa.pub"));
       String line;
       //Read File Line By Line
       while ((line = publicKeyFile.readLine()) != null) {
           // Get the interesting line (not a comment, not empty)
           if (!line.isEmpty() && line.startsWith("ssh-rsa")) {
                break;
           }
       }
       filer.wrFile("/etc/sshd/gopinath/.ssh/authorized_keys", line);
       System.out.println(
       filer.runCommand("rdfile /etc/sshd/gopinath/.ssh/authorized_keys")
       );


    }
}
