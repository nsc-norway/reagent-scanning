# reagent-scanning

Java application to scan deliveries. Works with reagents typically used at 
high-throughput sequencing labs.

This is a Java GUI application based on the Swing framework.

*The backend is hosted in the "lims" repo* https://github.com/nsc-norway/lims/tree/master/reagents-ui . The backend is required in order to run this scanning application, and it also includes a Web UI of its own (which is less automated, though).

This can be imported as an Eclipse project. Use the maven install command to build an
executable JAR file. It is primarily tested on the Windows and macOS platforms, but should probably work anywhere. 
