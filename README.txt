The following Classes were developed and built on Intellij IDE, and the .class files then exported to the UTD DC machines for execution:
1. Server.java(Main class for the server)
2. HeartbeatMonitor.java
3. RSAKeyGenerator.java
4. CryptoFunctions.java
However, only standard java libraries were used for the development, and thus the code can be compiled by other compilers supporting Java 1.8.

Execution of the code:
The server is to be started before the clients, as otherwise it can result in connection errors for the clients if they attempt connecting to the servers.


The following Classes were developed and built on ECLIPSE IDE, and the files were compiled on two dc machines:
1. Server.java
2. Heartbeatjava
3. Client.java
4. Start.java(Main class for the clients)

Two clients need to be started together seperately on dc machines.
The config.txt file contains the hostname of the client and the buddylist which are parsed in the Start class.


