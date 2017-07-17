KHopMetric Floodlight module
============================

KHopMetric is a Floodlight module to speed up the network convergence time after a fault.
The Floodlight controller implements the Openflow protocol, which specifications can be found here:
 [Openflow spec](http://www.openflow.org/documents/openflow-spec-v1.0.0.pdf)

This project depends on Floodlight, which can be found here:
 [Floodlight project on GitHub](https://github.com/floodlight/floodlight).

It has been tested with Mininet, which can be found here:
 [Mininet project on GitHub](https://github.com/mininet/mininet).
 
The module has been developed using the Yen-KShortestPath algorithm, which can be found here:
 [Yen-k-shortest-path project on Google code](http://code.google.com/p/k-shortest-paths/).


License
=======

This sofware is licensed under the Apache License, Version 2.0.

Information can be found here:
 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).


The K-Hop-Metric module
=======================

The K-Hop-Metric module is a modified version of the default Floodlight 0.90 Topology module. The dijkstra method in
the TopologyInstance class has been replaced with a new one, using the Yen-K-Shortest-Path algorithm to speed up the
convergence after a fault.
By default, after a fault, the Topology module recalculates all paths and broadcast trees from and to each node.
The new module uses a cache and precalculates K paths (where K is a number choosen by the network admin) per each couple
of nodes.

At setup time the module pre-calculates at most K-paths (if they exists) per each couple of nodes. 
After a disruptive change of the network (a link goes down or a switch turns off) the module choose the the best active
alternative path. When paths end the module starts to recalculate other K paths for that couplse of nodes.
After a costructive change of the network (adding a new OpenFlow device or making a new link between switches) the
module recalculate all paths per each couple of nodes.


Installation and configuration
==============================

This project has been developed and tested on Floodlight v0.90.

The module developed consists of the following components:
  * edu.asu.emit.qyan.alg.control package
  * edu.asu.emit.qyan.alg.model package
  * edu.asu.emit.qyan.alg.model.abstracts package

  * TopologyInstanceKHopMetric.java
  * TopologyManagerKHopMetric.java

Eclipse
-------

Using Eclipse, after the import of the Floodlight project:
  * Copy all edu.asu.emit.qyan.alg.* packages in the project folder (outside net.floodlightcontroller)
  * Copy TopologyInstanceKHopMetric.java and TopologyManagerKHopMetric.java in net.floodlightcontroller.topology
  * Modify the file src/main/resources/META-INF/services/net.floodlightcontroller.core.module.IFloodlightModule
    removing net.floodlightcontroller.topology.TopologyManager and adding
    net.floodlightcontroller.topology.TopologyManagerKHopMetric
  * Modify the file src/main/resources/floodlight.properties removing net.floodlightcontroller.topology.TopologyManager
    and adding net.floodlightcontroller.topology.TopologyManagerKHopMetric

Runnable file
-------------

For production environment, a jar version of the module is downloadable as well from the root directory of this
GitHub repository.

Alternatively, it is possible to create your own ``khopmetric.jar`` with the compiled files from this project.

According to Floodlight command sintax, you can integrate the jar file to your Floodlight installation running the
command:
```
java -cp floodlight.jar:khopmetric.jar net.floodlightcontroller.core.Main -cf floodlight.properties
```

The parameters specified have the following meaning:
 * ``floodlight.properties`` is the file specifying the properties for the running instance of Floodlight,
   it is configred to start the KHopMetric module provided with this project.
