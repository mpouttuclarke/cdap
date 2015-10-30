.. meta::
    :author: Cask Data, Inc.
    :copyright: Copyright © 2015 Cask Data, Inc.

:titles-only-toc: true

.. _faq-installation-startup:

==================================
CDAP FAQ: Installation and Startup
==================================

.. contents::
   :depth: 2
   :local:
   :backlinks: entry
   :class: faq


Building CDAP from Source
=========================

How can I build CDAP from source?
---------------------------------
Instructions for building CDAP from source are in the file ``BUILD.rst`` included in the
distribution. For example, this command will build the files required for a distribution:

.. literalinclude:: ../../../BUILD.rst
   :start-after: distributions (rpm, deb, tgz)
   :end-before: Cloudera Manager parcel
   :dedent: 4
      
It creates multiple output files (``tar.gz``\ ), located in the ``target`` directory
inside each of ``cdap-master``, ``cdap-kafka``, ``cdap-gateway``, and ``cdap-ui``.  The
``rpm`` and ``deb`` targets require that `fpm <https://github.com/jordansissel/fpm>`__ be
installed.

To build an SDK (suitable for Macintosh OS X, Windows, or Linux), use:

.. literalinclude:: ../../../BUILD.rst
   :start-after: Standalone distribution ZIP
   :end-before: the limited set of Javadocs used in distribution ZIP
   :dedent: 4


Installation
============

How do I install CDAP on CDH using Cloudera Manager?
----------------------------------------------------
We have a :ref:`tutorial <step-by-step-cloudera-add-service>` with instructions on how to
install CDAP on CDH (Cloudera) using Cloudera Manager. 

If, when you try to start services, you receive an error in ``stderr`` such as::
       
  Error found before invoking supervisord: No parcel provided required tags: set([u'cdap'])

The error message suggests that you have not completed the last step of installing a
parcel, *Activation*. There are 4 steps to installing a parcel:

- **Adding the repository** to the list of repositories searched by Cloudera Manager
- **Downloading** the parcel to the Cloudera Manager server
- **Distributing** the parcel to all the servers in the cluster
- **Activating** the parcel

Start by clicking on the parcel icon (near the top-left corner of Cloudera Manager; looks
like a gift-wrapped box), and ensure that the CDAP parcel is listed as *Active*.

How do I install CDAP on CDH without using Cloudera Manager?
------------------------------------------------------------
If you have a CDH cluster not managed by Cloudera Manager, you can install CDAP by following
the manual instructions. Note that per the Software Prerequisites, a configured Hadoop, HBase,
and optionally Hive client need to be configured on the node(s) where CDAP will run.

How do I install CDAP on HDP using Ambari?
------------------------------------------
Instructions on installing CDAP on HDP using Apache Ambari :ref:`are available <ambari-configuring>`.

How do I install CDAP on HDP without using Ambari?
--------------------------------------------------
If you have an HDP cluster not managed by Ambari, you can install CDAP by following
the manual instructions. Note that per the Software Prerequisites, a configured Hadoop, HBase,
and optionally Hive client need to be configured on the node(s) where CDAP will run.

Note that HDP version 2.2 and above require some additional configuration <link>.

How do I install CDAP on MapR?
------------------------------
(perhaps this needs its own section in the docs to link to here)

CDAP can be installed on a MapR cluster by following the manual instructions. Note that per the
Software Prerequisites, a configured Hadoop, HBase, and optionally Hive client need to be
configured on the node(s) where CDAP will run.

To configure a MapR Hadoop client, see http://doc.mapr.com/display/MapR/Setting+Up+the+Client.

To configure a MapR HBase client, see
http://doc.mapr.com/display/MapR/Installing+HBase#InstallingHBase-HBaseonaClientInstallingHBaseonaClient

To configure a Mapr Hive client, see http://doc.mapr.com/display/MapR/Installing+Hive

A typical client node should have the mapr-client, mapr-hbase, and mapr-hive packages installed, and
be configured using the mapr configure.sh utility.

As in all installations, the kafka.log.dir may need to be created locally.

MapR does not provide a configured ``yarn.application.classpath`` by default. CDAP requires an additional
entry, ``/opt/mapr/lib/*`` be appended to the ``yarn.application.classpath`` setting in ``yarn-site.xml``.
The default ``yarn.application.classpath`` for Linux with this additional entry appended is:

$HADOOP_CONF_DIR, $HADOOP_COMMON_HOME/share/hadoop/common/*, $HADOOP_COMMON_HOME/share/hadoop/common/lib/*, $HADOOP_HDFS_HOME/share/hadoop/hdfs/*, $HADOOP_HDFS_HOME/share/hadoop/hdfs/lib/*, $HADOOP_YARN_HOME/share/hadoop/yarn/*, $HADOOP_YARN_HOME/share/hadoop/yarn/lib/*, $HADOOP_COMMON_HOME/share/hadoop/mapreduce/*, $HADOOP_COMMON_HOME/share/hadoop/mapreduce/lib/*, /opt/mapr/lib/*

Note that MapR may not dereference the hadoop variables ($HADOOP_CONF_DIR, etc), so we recommend specifying their full paths.


How do I install CDAP on Apache Hadoop?
---------------------------------------
You can use either our :ref:`quick start installation instructions
<installation-quick-start>`, or if you have either an unusual situation (such as HA [high
availability]) or would like more details on the steps and the available options, follow
our :ref:`general installation instructions <install>`.


Startup
=======

CDAP does not startup
---------------------
If you have followed :ref:`the installation instructions <install>`, and CDAP either did not pass the 
:ref:`verification step <configuration-verification>` or did not startup, check:

- Look in the CDAP logs for error messages (located in ``$CDAP_HOME/logs``)
- If you see an error such as::

    ERROR [main:c.c.c.StandaloneMain@268] - Failed to start Standalone CDAP
    java.lang.NoSuchMethodError: 
    co.cask.cdap.UserInterfaceService.getServiceName()Ljava/lang/String

  then you have probably downloaded the standalone version of CDAP, which is not intended
  to be run on Hadoop clusters. Download the appropriate distributed packages (RPM or
  Debian version) from http://cask.co/downloads.
         
- Check permissions of directories and network configuration errors
- Check :ref:`configuration troubleshooting <configuration-troubleshooting>` suggestions
    
CDAP UI shows a blank screen
----------------------------
TBC.



CDAP UI shows a message "namespace cannot be found"
---------------------------------------------------
This is indicative that the UI cannot connect to the CDAP system service containers running in
YARN.

First check if the CDAP Master service container shows as RUNNING in the YARN ResourceManager UI (link to entry below)

If this doesn't resolve the issue, then it means the CDAP system services are unable to launch.  Ensure YARN has enough
spare memory and vcore capacity.  CDAP attempts to launch between 8 - 11 containers depending on configuration. Check
the master container (Application Master) logs to see if it is able to launch all containers.

If it is able to launch all containers, then you may need to further check the launched container logs for any errors.

CDAP UI shows a session time out
--------------------------------
TBC.



I don't see the CDAP Master service on YARN
-------------------------------------------
Ensure the node where CDAP is running has a properly configured YARN client.
Ensure YARN has enough memory and vcore capacity.


CDAP Master log shows permissions issues
----------------------------------------
Ensure that hdfs:///#{hdfs.namespace} and hdfs:///user/#{hdfs.user} exist and are owned by #{hdfs.user}.
In rare cases, until CDAP-3817 is resolved, ensure hdfs:///#{hdfs.namespace}/tx.snapshot exists and is
owned by #{hdfs.user}. In any other case, the error should show which directory it is attempting to
access. Don't hesitate to ask for help <link>



CDAP Master log shows an error about the dataset service not found
------------------------------------------------------------------
If you see an error such as::

    2015-05-15 12:15:53,028 - ERROR [heartbeats-scheduler:c.c.c.d.s.s.MDSStreamMetaStore$1@71] 
    - Failed to access app.meta table co.cask.cdap.data2.dataset2.DatasetManagementException: 
    Cannot retrieve dataset instance app.meta info, details: Response code: 407, 
    message:'Proxy Authentication Required', 
    body: '<HTML><HEAD> <TITLE>Access Denied</TITLE> </HEAD>
    
According to that log, this error can be caused by a proxy setting. CDAP services
internally makes HTTP requests to each other; one example is the dataset service.
Depending on your proxy and its settings, these requests can end up being sent to the
proxy instead.

One item to check is that your system's network setting is configured to exclude both
``localhost`` and ``127.0.0.1`` from the proxy routing. If they aren't, the services will
not be able to communicate with each other, and you'll see error messages such as these.

Where is the CDAP CLI (Command Line Interface)?
-----------------------------------------------
If you've installed the ``cdap-cli`` RPM or Deb, it's located under ``/opt/cdap/cli/bin``.
You can add this location to your PATH to prevent the need for specifying the entire script every time.

**Note:** These commands will list the contents of the package ``cdap-cli``, once it has
been installed::

  rpm -ql cdap-cli
  dpkg -L cdap-cli

What are the memory and core requirements for CDAP?
---------------------------------------------------
The settings are governed by two sources: CDAP and YARN. The default setting for CDAP are
found in the ``cdap-defaults.xml``, and are over-ridden in particular instances by the
``cdap-site.xml`` file. These vary with each service and range from 512 to 1024 MB and
from one to two cores.

The YARN settings will over-ride these; for instance, the minimum YARN container size is
determined by ``yarn.scheduler.minimum-allocation-mb``. The YARN default in ``HDP/Hadoop``
is 1024 MB, so containers will be allocated with 1024 MB, even if the CDAP settings are
for 512 MB.

Can a current CDAP installation be upgraded more than one version?
------------------------------------------------------------------
This table lists the upgrade paths available for different CDAP versions:

+---------+---------------------+
| Version | Upgrade Directly To |
+=========+=====================+
| 2.6.3   | 2.8.1               |
+---------+---------------------+

If you are doing a new installation, we recommend using the current version of CDAP.


Configuring Distributed Mode
============================

Are at least two machines really required?
------------------------------------------
The CDAP components are independently scalable, so you can install from 1 to *N* of each
component on any combination of nodes.  The primary reasons for using at least two
machines are for HA (high availability) and for ``cdap-router``'s data ingest capacity.

It is not necessary to install all components on both machines; you could install just the
CDAP UI on a third machine with other components on the second node. You can install each
component on a separate machine (or more) if you choose. The :ref:`HA [High Availability]
Environment diagram <deployment-architectures-ha>` gives just one possible
configuration.

My Hive Server2 defaults to 10000; what should I do?
----------------------------------------------------
If port 10000 is being used by another service, simply change ``router.bind.port`` in
the ``cdap-site.xml`` to another available port. Since in the Hadoop ecosystem, Hive
Server2 defaults to 10000, we are considering changing the router default port. 
       
How do I set the CDAP properties for components running on multiple machines?
-----------------------------------------------------------------------------
In the configuration file ``cdap-site.xml``, there are numerous properties that specify an
IP address where a service is running, such as ``router.server.address``,
``metrics.query.bind.address``, ``data.tx.bind.address``, ``app.bind.address``,
``router.bind.address``.
       
Our convention is that:

- *\*.bind.\** properties are what services use during startup to listen on a particular interface/port.  
- *\*.server.\** properties are used by clients to connect to another (potentially remote) service.

For *\*.bind.address* properties, it is often easiest just to set these to ``'0.0.0.0'``
to listen on all interfaces.

The *\*.server.\** properties are used by clients to connect to another remote service.
The only one you should need to configure initially is ``router.server.address``, which is
used by the UI to connect to the router.  As an example, ideally routers running in
production would have a load balancer in front, which is what you would set
``router.server.address`` to. Alternatively, you could configure each UI instance to point
to a particular router, and if you have both UI and router running on each node, you could
use ``'127.0.0.1'``.


Additional Resources
====================

Ask the CDAP Community for assistance
-------------------------------------

.. include:: cdap-user-googlegroups.txt
