package netappfiler;

/**
 *
 * @author Srinivas Gopinath Parimi
 */
import java.io.*;
import java.util.*;
import java.util.regex.*;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.jcraft.jsch.*;


public class NetAppFiler {
    
    String hostname;
    String username;
    String password;
                
    public NetAppFiler(String fName, String fUser, String fPasswd) {
        hostname = fName;
        username = fUser;
        password = fPasswd;
}
                
    public String runCommand(String cmd) {
        
        StringBuilder output = new StringBuilder();

        try {
	      /* Create a connection instance */
              Connection conn = new Connection(hostname);
              conn.connect();
              boolean isAuthenticated = 
                      conn.authenticateWithPassword(username, password);
              if (isAuthenticated == false) {
                  throw new IOException("Authentication failed.");
              }
				
              ch.ethz.ssh2.Session sess = conn.openSession();
              sess.execCommand(cmd);

	      InputStream stdout = new StreamGobbler(sess.getStdout());
              BufferedReader br = 
                      new BufferedReader(new InputStreamReader(stdout));
              
              String line = br.readLine();
              while (line != null) {
                  output.append(line);
                  output.append("\n");
                  line = br.readLine();              
              }
              sess.close();
              conn.close();
            } catch (IOException e) {
                  output.append(e.toString());
                  output.append("\n");
            }
        return output.toString();
    }                
    
    public HashMap<String, String> getOptions() {
      HashMap<String, String> options = new HashMap<String, String>();
      String optionsList = runCommand("options");
      if(optionsList.contains("Authentication fail")) {
          return new HashMap<String, String>();
      }
      StringBuilder sb = new StringBuilder(optionsList);
        
        try {
              StringReader sr = new StringReader(sb.toString());
              BufferedReader br = new BufferedReader(sr);
              String line = new String();
              
              while ((line = br.readLine()) != null) {
                  
                      String[] statusLine = 
                              line.replaceAll("\\s+", " ").split(" ");

                    if(statusLine.length >= 2) {
                        options.put(statusLine[0], statusLine[1]);                        
                     }
                   }
            } catch (Exception e) {
                    System.out.println("Exception while retrieving " + 
                            "options: " + e.toString());
                    
            }      
      return options;
    }        
    
    public HashMap aggrStatus() {
        HashMap aggrStatusTable = new HashMap();
        StringBuilder sb = new StringBuilder(runCommand("aggr status"));
        
        try {
              StringReader sr = new StringReader(sb.toString());
              BufferedReader br = new BufferedReader(sr);
              String result = "does not exist";
              String line = new String();
              
              while ((line = br.readLine().trim()) != null) {
                  
                    if (line.matches(".*" + "Aggr" + ".*" + "State" + ".*")) {
                        continue;
                       }
                    String[] statusLine = line.split(" ");
                    result = statusLine[1];
                    aggrStatusTable.put(statusLine[0], result);
                    
                    }
            } catch (Exception e) {
                    System.out.println("Exception while retrieving " + 
                            "list of aggregates: " + e.toString());
                    
            }
        return aggrStatusTable;
    }
    
    
    
    public HashMap aggrStatus(List<String> aggrList) {
        HashMap aggrStatusTable = new HashMap();
        StringBuilder sb = new StringBuilder(runCommand("aggr status"));
        
        for(String aggrName : aggrList) {
        try {
              StringReader sr = new StringReader(sb.toString());
              BufferedReader br = new BufferedReader(sr);
              String result = "does not exist";
              String line = new String();
              
              while ((line = br.readLine().trim()) != null) {
                  
                    if (line.matches(".*" + aggrName + ".*")) {
                        String[] statusLine = line.split(" ");
                        result = statusLine[1];
                        aggrStatusTable.put(aggrName, result);
                        break;
                       }
                    
                    }
            } catch (Exception e) {
                    aggrStatusTable.put(aggrName, "does not exist");
            }
         } // for loop
        return aggrStatusTable;
    }
    
    
    
    public String aggrStatus(String aggrName) {
        
        String result = runCommand("aggr status " + aggrName);
        if (result.isEmpty()) {
            return "does not exist";
        }
        
        try {
               StringReader sr = new StringReader(result);
               BufferedReader br = new BufferedReader(sr);
               String line = new String();
               
               while ((line = br.readLine()) != null) {
                 line = line.trim();
                 if (line.isEmpty()) {
                     continue;
                 }
                 if (line.startsWith(aggrName)) {
                     String[] statusLine = line.split(" ");
                     return statusLine[1];
                    } 
                 }
            } catch (Exception e) {
                 System.out.println(e.toString());
            }
        return result;
    }
         

    
    public boolean isOnline(String entity) {
        
        String p = new String(entity + " online");
        CharSequence cs = p.subSequence(0, (p.length() - 1));
        String result = runCommand("aggr status " + entity + 
                "; vol status " + entity);
        if(result.contains(cs)) {
            return true;
        }
        return false;
    }
    
    
    public boolean has(String entity) {
        
        String result = runCommand("aggr status " + entity + 
                " ; vol status " + entity);
        if (result.isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    
    public boolean snapshotExists(String volumeName, String snapshotName) {
        if (!isOnline(volumeName)) {
            return false;
        }
        
        String result = runCommand("snap list " + volumeName);
        if (result.contains(snapshotName)) {
            return true;
        }
        return false;
    }
    
    
    public String volSize(String volName) {

        String size = "not found";
        String result = runCommand("vol size " + volName);
        if (!result.contains("No volume")) {
            String[] resultLine = result.split("has size");
            size = resultLine[1].replace('.', ' ').trim();
        }
        return size;
    }
    
    
        public long aggrFreeSpace(String aggrName) {
        // Free space in KB
        long freeSpace = 0;
        if(!isOnline(aggrName)) {
            return 0;
        }
        
        StringBuilder sb = new StringBuilder(
                runCommand("aggr show_space " + aggrName));
        try {
              StringReader sr = new StringReader(sb.toString());
              BufferedReader br = new BufferedReader(sr);
              String line = new String();
              
              while ((line = br.readLine()) != null) {
                  line = line.trim();
                  
                  if (line.isEmpty()) {
                      continue;
                    }
                  Pattern p = Pattern.compile("\\s+");
                  if (line.matches("Total space" + ".*" + "KB")) {
                      
                      String[] statusLine = 
                              line.replaceAll("\\s+", " ").split(" ");
                      System.out.println("Space = " + statusLine[4]);
                      freeSpace = Long.parseLong(
                              statusLine[4].replaceAll("KB", "").trim());
                      break;
                    } else {
                      continue;
                    }
                 }
            } catch (Exception e) {
                    System.out.println("Exception while retrieving "
                            + "list of aggregates: " + e.toString());
                    return -1;
                    
            }
        return freeSpace;
    }

    
    
public List<String> getVolumes(String aggrName) {
    List<String> volumeList = new ArrayList<String>();
    StringBuilder sb = new StringBuilder(
            runCommand("aggr show_space -h " + aggrName));
       
    try {
          StringReader sr = new StringReader(sb.toString());
          BufferedReader br = new BufferedReader(sr);
          String line = new String();
              
          while ((line = br.readLine()) != null) {
                 line = line.trim();
                 if (line.isEmpty()) {
                     continue;
                 }
                 if (line.matches(".*" + "Aggregate" + ".*") || 
                     line.matches("[0-9]+.*") ||
                     line.startsWith("Total space") || 
                     line.startsWith("Space allocated") || 
                     line.endsWith("Guarantee") || 
                     line.startsWith("Snap reserve") ||
                     line.startsWith("WAFL reserve")) {
                        continue;
                    } 
                    String[] statusLine = line.split(" ");
                    volumeList.add(statusLine[0]);
                    }
            } catch (Exception e) {
                    System.out.println("Exception while retrieving "
                            + "list of aggregates: " + e.toString());
                    
            }
        return volumeList;
    }


public List<String> getAllFlexibleVolumes() {
    List<String> volumeList = new ArrayList<String>();
    StringBuilder sb = new StringBuilder(runCommand("vol status"));
       
    try {
          StringReader sr = new StringReader(sb.toString());
          BufferedReader br = new BufferedReader(sr);
          String line = new String();
              
          while ((line = br.readLine()) != null) {
                 line = line.trim();
                 if (line.isEmpty()) {
                     continue;
                 }
                 if (line.trim().endsWith("Options") || line.contains(", trad")) {
                        continue;
                    } 
                    String[] statusLine = line.split(" ");
                    volumeList.add(statusLine[0]);
                    }
            } catch (Exception e) {
                    System.out.println("Exception while retrieving "
                            + "list of volumes: " + e.toString());
                    
            }
        return volumeList;
    }

public List<String> getAllTraditionalVolumes() {
    List<String> volumeList = new ArrayList<String>();
    StringBuilder sb = new StringBuilder(runCommand("vol status"));
       
    try {
          StringReader sr = new StringReader(sb.toString());
          BufferedReader br = new BufferedReader(sr);
          String line = new String();
              
          while ((line = br.readLine()) != null) {
                 line = line.trim();
                 if (line.isEmpty()) {
                     continue;
                 }
                 if (!line.contains(", trad")) {
                        continue;
                    } 
                    String[] statusLine = line.split(" ");
                    volumeList.add(statusLine[0]);
                    }
            } catch (Exception e) {
                    System.out.println("Exception while retrieving "
                            + "list of volumes: " + e.toString());
                    
            }
        return volumeList;
    }





public List<String> getAggregates() {
    List<String> aggrList = new ArrayList<String>();
    StringBuilder sb = new StringBuilder(runCommand("aggr status"));
       
    try {
          StringReader sr = new StringReader(sb.toString());
          BufferedReader br = new BufferedReader(sr);
          String line = new String();
              
          while ((line = br.readLine()) != null) {
                 line = line.trim();
                 if (line.isEmpty()) {
                     continue;
                 }
                 if (line.matches(".*" + "Aggr" + ".*" + "State" + ".*")) {
                        continue;
                 } 
                 
                 if (line.trim().matches(".*" + ",.*" + "trad" + ".*")) {
                        continue;
                 }                  
                 
                 if (line.trim().startsWith("32")) {
                        continue;
                 }                  
                 
                 if (line.trim().startsWith("64")) {
                        continue;
                 }                        
                 
                    String[] statusLine = line.split(" ");
                    aggrList.add(statusLine[0]);
                    }
            } catch (Exception e) {
                    System.out.println("Exception while retrieving "
                            + "list of aggregates: " + e.toString());
                    
            }
        return aggrList;
    }

public String getRootAggregate() {
    String aggrName = new String();
    StringBuilder sb = new StringBuilder(runCommand("aggr status"));
       
    try {
          StringReader sr = new StringReader(sb.toString());
          BufferedReader br = new BufferedReader(sr);
          String line = new String();
              
          while ((line = br.readLine()) != null) {
                 line = line.trim();
                 if (line.isEmpty()) {
                     continue;
                 }
                 if (line.matches(".*" + "Aggr" + ".*" + "State" + ".*")) {
                        continue;
                 } 
                 
                 if (line.trim().endsWith("root")) {
                    String[] statusLine = line.split(" ");
                    aggrName = statusLine[0];
                    break;
                 }
                    }
            } catch (Exception e) {
                    System.out.println("Exception while retrieving "
                            + "list of aggregates: " + e.toString());
                    
            }
        return aggrName;
    }

  
public String getRootVolume() {
    String aggrName = new String();
    StringBuilder sb = new StringBuilder(runCommand("aggr status"));
       
    try {
          StringReader sr = new StringReader(sb.toString());
          BufferedReader br = new BufferedReader(sr);
          String line = new String();
              
          while ((line = br.readLine()) != null) {
                 line = line.trim();
                 if (line.isEmpty()) {
                     continue;
                 }
                 if (line.matches(".*" + "Aggr" + ".*" + "State" + ".*")) {
                        continue;
                 } 
                 
                 if (line.trim().endsWith("root")) {
                    String[] statusLine = line.split(" ");
                    aggrName = statusLine[0];
                    break;
                 }
                    }
            } catch (Exception e) {
                    System.out.println("Exception while retrieving "
                            + "list of aggregates: " + e.toString());
                    
            }
        return aggrName;
    }

    
    
    public boolean aggrDestroy(String aggrName) {
        
        runCommand("aggr online " + aggrName);
        List<String> volumes = getVolumes(aggrName);
        
        for (String v : volumes) {
            String result = runCommand("vol offline " + v + 
                    "; vol destroy " + v + " -f");
            if (result.contains("cannot")) {
                System.out.println("Volume" + v +  " could not be destroyed.");
                return false;
            }
        }
        
        String result = runCommand("aggr offline " + aggrName + 
                "; aggr destroy " + aggrName + " -f");
        runCommand("disk zero spares");
        if(result.contains("cannot") || result.contains("can't")) {
            System.out.println("Aggregate " + aggrName + 
                    " could not be destroyed");
            return false;
        }
        return true;
    }
   
public boolean destroyVolumes(String aggrName) {
        
        boolean returnValue = true;
        runCommand("aggr online " + aggrName);
        StringBuilder cmdBuf = new StringBuilder();
        
        List<String> volumes = getVolumes(aggrName);
        
        for (String v : volumes) {
            
            String cmd = "vol offline " + v + 
                    " ; vol destroy " + v + " -f ; " ;
            cmdBuf.append(cmd);
        }
        
        if (cmdBuf.capacity() < 1024) {
            
           String result = runCommand(cmdBuf.toString());
           return !(result.contains("cannot") || result.contains("can't"));
        } else {
            System.out.println("Too many volumes! "
                    + "Will delete one by one separately!");
            for (String v : volumes) {
                returnValue = destroyVolume(v);
            }
            
        }
        return returnValue;
    }
    

public boolean destroyVolume(String volName) {
  String result = runCommand("vol offline " + volName + 
                    " ; vol destroy " + volName + " -f ; ");
  if (result.contains("cannot") || result.contains("can't")) {
      return false;
     }
  return true;
}



    public boolean wrFile(String dstPath, String content) {
        content += "\n";
        try {
	      /* Create a connection instance */
              Connection conn = new Connection(hostname);
              conn.connect();
              boolean isAuthenticated = 
                      conn.authenticateWithPassword(username, password);
              if (isAuthenticated == false) {
                  throw new IOException("Authentication failed.");
              }
				
              ch.ethz.ssh2.Session sess = conn.openSession();
              
              sess.requestPTY("VT100");
              sess.startShell();

              OutputStream sshOut = sess.getStdin();

              String cmd = "wrfile " + dstPath + "\n";
              sshOut.write(cmd.getBytes());
              
              
           BufferedReader lines = new BufferedReader(new StringReader(content));
           
           String line = new String();
           while ((line = lines.readLine()) != null) {
               line += "\n";
               sshOut.write(line.getBytes());
               try {
                   Thread.sleep(500);
               } catch(Exception e) {
                   
               }
               //sshOut.flush();
              }
              
              Character c = 3;
              sshOut.write(c);
              sshOut.flush();
              sshOut.close();
              sess.close();
              conn.close();
            } catch (IOException e) {
                  System.out.println(e.toString());
                  return false;
            }
        return true;
    } 
    
 
    
public String jsh(String command) {
    String result = new String();
    try {
           JSch jsch = new JSch();
           com.jcraft.jsch.Session session = jsch.getSession(username, hostname);
           session.setPassword(password);
           session.setConfig("StrictHostKeyChecking", "no");
           session.connect();
           ChannelShell channel = (ChannelShell)session.openChannel("shell");
           
           OutputStream stdOut =channel.getOutputStream();
           InputStream in = channel.getInputStream();
           channel.connect();

            // read command output
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            stdOut.write("java netapp.cmds.jsh\n".getBytes());
            command += "\n";
            stdOut.write(command.getBytes());
            stdOut.write("exit\n".getBytes());

            stdOut.flush(); 

            result = getstuff(in,buffer,bos,channel);
            System.out.println(result);
            stdOut.flush();
            channel.disconnect();
        } catch (JSchException jse) {
            // TODO: Add catch code
            jse.printStackTrace();
        } catch (IOException ioe) {

            ioe.printStackTrace();
        }
    return result;
    }
    
public String getstuff(InputStream in,byte[] buffer, ByteArrayOutputStream
bos, ChannelShell channel){

      try {
            final long endTime = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < endTime) {
                while (in.available() > 0) {
                    int count = in.read(buffer, 0, 1024);
                    if (count >= 0) {
                        bos.write(buffer, 0, count);
                    } else {
                        break;
                    }
                }
                if (channel.isClosed()) {
                    //int exitStatus = channel.getExitStatus();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //("Ignoring interrupt.");
                }
            }
        } catch (IOException ioe) {
            // TODO: Add catch code
            ioe.printStackTrace();
        }
        String stuff = bos.toString();
        bos.reset();
        return stuff;
    }   
    
    
    
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
                          new FileReader("/home/srinigp/.ssh/id_rsa.pub"));
       String line;
       //Read File Line By Line
       while ((line = publicKeyFile.readLine()) != null) {
           // Get the interesting line (not a comment, not empty)
           if (!line.isEmpty() && line.startsWith("ssh-rsa")) {
                break;
           }
       }
       filer.wrFile("/etc/sshd/syncuser_smurf/.ssh/authorized_keys", line);
       System.out.println(
       filer.runCommand("rdfile /etc/sshd/syncuser_smurf/.ssh/authorized_keys")
       );


    }
    
}

