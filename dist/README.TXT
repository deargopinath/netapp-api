netappfiler


- Java based API to access, configure and manage NetApp DATA ONTAP filers

- Ideal tool for System Administrators of NetApp storage systems.

- Extremely useful for the developers of GUI or command line tools for managing NetApp filers.

- uses SSH internally to access the filers.


How to use the API?
------------------

It is very simple.

1. Import the relevant packages

   import netapp.*;
   import java.io.*;
   import java.util.*;


2. Initialize NetAppFiler object

   NetAppFiler filer = new NetAppFiler("hostname_or_ip_address", 
                                        "ssh_user_name", "ssh_password");


3. Call the relevant function on the NetAppFiler object

   String output = filer.runCommand("aggr status");

   if(filer.has("vol1")) {
     System.out.prinltn("Volume named vol1 exists");
   }
   


The sample program - NetAppSample.java shows a sample use of the API.

Please refer the javadoc/index.html file for the complete list of functions supported by the API.




How to obtain support?
----------------------

Please feel free to write to me for any assistance.
I will be happy to help.


Yours sincerely,

Gopinath

deargopinath@yahoo.com







