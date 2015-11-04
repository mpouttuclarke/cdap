.. meta::
    :author: Cask Data, Inc.
    :copyright: Copyright © 2014-2015 Cask Data, Inc.

.. _install:

============
Installation
============

.. Note: this file is included in quick-start.rst; check any edits in this file with it!

Introduction
------------

This manual is to help you install the Cask Data Application Platform (CDAP). It provides the
`system <#system-requirements>`__, 
`network <#network-requirements>`__, and
`software requirements <#software-prerequisites>`__,
`packaging options <#packaging>`__, and
instructions for
`installation <#installation>`__ of
the CDAP components so they work with your existing Hadoop cluster.

There are specific instructions for :ref:`upgrading existing CDAP installations <configuration-upgrade>`.

Once installed, another page :ref:`covers configuration <configuration>` of the CDAP installation.

.. _install-follow-these-instructions:

Follow these instructions only if you aren't using a cluster manager such as :ref:`Cloudera
Manager <cloudera-configuring>` or :ref:`Apache Ambari <ambari-configuring>`. For the Mapr
distribution, we have :ref:`additional instructions <mapr-configuring>` to start with;
please read those before continuing. These instructions *do not apply* to the
:ref:`standalone SDK <standalone-index>`

.. _install-these-are-the-cdap-components:

These are the CDAP components:

- **CDAP UI:** User interface—the *Console*—for managing CDAP applications;
- **CDAP Router:** Service supporting REST endpoints for CDAP;
- **CDAP Master:** Service for managing runtime, lifecycle and resources of CDAP applications;
- **CDAP Kafka:** Metrics and logging transport service, using an embedded version of *Kafka*; and
- **CDAP Authentication Server:** Performs client authentication for CDAP when security is enabled.

Before installing the CDAP components, you must first install a Hadoop cluster
with *HDFS*, *YARN*, *HBase*, and *ZooKeeper*. In order to use the ad-hoc querying capabilities
of CDAP, you will also need *Hive*. All CDAP components can be installed on the
same boxes as your Hadoop cluster, or on separate boxes that can connect to the Hadoop services.

Our recommended installation is to use two boxes for the CDAP components; the
`hardware requirements <#hardware-requirements>`__ are relatively modest,
as most of the work is done by the Hadoop cluster. These two
boxes provide high availability; at any one time, one of them is the leader
providing services while the other is a follower providing failover support.

Some CDAP components run on YARN, while others orchestrate “containers” in the Hadoop cluster.
The CDAP Router service starts a router instance on each of the local boxes and instantiates
one or more gateway instances on YARN as determined by the gateway service configuration.

We have specific
`hardware <#hardware-requirements>`_,
`network <#network-requirements>`_ and
`prerequisite software <#software-prerequisites>`_ requirements detailed
`below <#system-requirements>`__
that need to be met and completed before installation of the CDAP components.


System Requirements
-------------------

.. _install-hardware-requirements:

Hardware Requirements
.....................
Systems hosting the CDAP components must meet these hardware specifications,
in addition to having CPUs with a minimum speed of 2 GHz:

+---------------------------------------+--------------------+-----------------------------------------------+
| CDAP Component                        | Hardware Component | Specifications                                |
+=======================================+====================+===============================================+
| **CDAP UI**                           | RAM                | 1 GB minimum, 2 GB recommended                |
+---------------------------------------+--------------------+-----------------------------------------------+
| **CDAP Router**                       | RAM                | 2 GB minimum, 4 GB recommended                |
+---------------------------------------+--------------------+-----------------------------------------------+
| **CDAP Master**                       | RAM                | 2 GB minimum, 4 GB recommended                |
+---------------------------------------+--------------------+-----------------------------------------------+
| **CDAP Kafka**                        | RAM                | 1 GB minimum, 2 GB recommended                |
+                                       +--------------------+-----------------------------------------------+
|                                       | Disk Space         | *CDAP Kafka* maintains a data cache in        |
|                                       |                    | a configurable data directory.                |
|                                       |                    | Required space depends on the number of       |
|                                       |                    | CDAP applications deployed and running        |
|                                       |                    | in the CDAP and the quantity                  |
|                                       |                    | of logs and metrics that they generate.       |
+---------------------------------------+--------------------+-----------------------------------------------+
| **CDAP Authentication Server**        | RAM                | 1 GB minimum, 2 GB recommended                |
+---------------------------------------+--------------------+-----------------------------------------------+


.. _install-hardware-memory-core-requirements:

Memory and Core Requirements
............................
Memory and core requirements are governed by two sources: CDAP and YARN. The default
settings for CDAP are found in the :ref:`cdap-defaults.xml <appendix-cdap-default.xml>`,
and are overridden in particular instances by the :ref:`cdap-site.xml
<appendix-cdap-site.xml>` file. These vary with each service and range from 512 to 1024 MB
and from one to two cores.

The YARN settings will override these; for instance, the minimum YARN container size is
determined by ``yarn.scheduler.minimum-allocation-mb``. The YARN default in Hadoop is 1024
MB, so containers will be allocated with 1024 MB, even if the CDAP settings are for 512 MB.

With the default YARN settings, CDAP can require from 14 to 16 GB of memory, available to
YARN, just to start.

.. _install-network-requirements:

Network Requirements
....................
CDAP components communicate over your network with *HBase*, *HDFS*, and *YARN*.
For the best performance, CDAP components should be located on the same LAN,
ideally running at 1 Gbps or faster. A good rule of thumb is to treat CDAP
components as you would *Hadoop DataNodes*.  

.. _install-software-requirements:

Software Prerequisites
......................
You'll need this software installed:

- Java runtime (on CDAP and Hadoop nodes)
- Node.js runtime (on CDAP nodes)
- Hadoop and HBase (and optionally Hive) environment to run against
- CDAP nodes require Hadoop and HBase client installation and configuration. 
  **Note:** No Hadoop services need to be running.

.. highlight:: console

.. _install-java-runtime:

Java Runtime
++++++++++++
The latest `JDK or JRE version 1.7.xx or 1.8.xx <http://www.java.com/en/download/manual.jsp>`__
for Linux, Windows, or Mac OS X must be installed in your environment; we recommend the Oracle JDK.

To check the Java version installed, run the command::

  $ java -version
  
CDAP is tested with the Oracle JDKs; it may work with other JDKs such as 
`Open JDK <http://openjdk.java.net>`__, but it has not been tested with them.

Once you have installed the JDK, you'll need to set the JAVA_HOME environment variable.


.. _install-node.js:

Node.js Runtime
+++++++++++++++
You can download the appropriate version of Node.js from `nodejs.org <http://nodejs.org>`__:

#. The version of Node.js must be from |node-js-version|; we recommend |recommended-node-js-version|.
#. Download the appropriate binary ``.tar.gz`` from
   `nodejs.org/download/ <http://nodejs.org/dist/>`__.

#. Extract somewhere such as ``/opt/node-[version]/``
#. Build node.js; instructions that may assist are available at
   `github <https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager>`__
#. Ensure that ``nodejs`` is in the ``$PATH``. One method is to use a symlink from the installation:
   ``ln -s /opt/node-[version]/bin/node /usr/bin/node``

.. _install-hadoop-hbase:

Hadoop/HBase Environment
++++++++++++++++++++++++

For a distributed enterprise, you must install these Hadoop components:

+---------------+-------------------+-----------------------------------------------------+
| Component     | Source            | Supported Versions                                  |
+===============+===================+=====================================================+
| **HDFS**      | Apache Hadoop     | 2.0.2-alpha through 2.6.0                           |
+               +-------------------+-----------------------------------------------------+
|               | CDH or HDP        | (CDH) 5.0.0 through 5.4.x or (HDP) 2.0 through 2.3  |
+               +-------------------+-----------------------------------------------------+
|               | MapR              | 4.1 and 5.0 (with MapR-FS)                          |
+---------------+-------------------+-----------------------------------------------------+
| **YARN**      | Apache Hadoop     | 2.0.2-alpha through 2.6.0                           |
+               +-------------------+-----------------------------------------------------+
|               | CDH or HDP        | (CDH) 5.0.0 through 5.4.x or (HDP) 2.0 through 2.3  |
+               +-------------------+-----------------------------------------------------+
|               | MapR              | 4.1 and 5.0                                         |
+---------------+-------------------+-----------------------------------------------------+
| **HBase**     | Apache            | 0.96.x, 0.98.x, and 1.0.x                           |
+               +-------------------+-----------------------------------------------------+
|               | CDH or HDP        | (CDH) 5.0.0 through 5.4.x or (HDP) 2.0 through 2.3  |
+               +-------------------+-----------------------------------------------------+
|               | MapR              | 4.1 and 5.0 (with Apache HBase)                     |
+---------------+-------------------+-----------------------------------------------------+
| **ZooKeeper** | Apache            | Version 3.4.3 through 3.4.5                         |
+               +-------------------+-----------------------------------------------------+
|               | CDH or HDP        | (CDH) 5.0.0 through 5.4.x or (HDP) 2.0 through 2.3  |
+               +-------------------+-----------------------------------------------------+
|               | MapR              | 4.1 and 5.0                                         |
+---------------+-------------------+-----------------------------------------------------+
| **Hive**      | Apache            | Version 0.12.0 through 0.13.1                       |
+               +-------------------+-----------------------------------------------------+
|               | CDH or HDP        | (CDH) 5.0.0 through 5.4.x or (HDP) 2.0 through 2.3  |
+               +-------------------+-----------------------------------------------------+
|               | MapR              | 4.1 and 5.0                                         |
+---------------+-------------------+-----------------------------------------------------+

**Note:** Components versions shown in this table are those that we have tested and are
confident of their suitability and compatibility. Later versions of components may work,
but have not necessarily have been either tested or confirmed compatible.

**Note:** Certain CDAP components need to reference your *Hadoop*, *HBase*, *YARN* (and
possibly *Hive*) cluster configurations by adding your configuration to their class paths.

**Note:** ZooKeeper's ``maxClientCnxns`` must be raised from its default.  We suggest setting it to zero
(unlimited connections). As each YARN container launched by CDAP makes a connection to ZooKeeper, 
the number of connections required is a function of usage.

**Note:** *Hive 0.12* is not supported for :ref:`secure cluster configurations <configuration-security>`.


.. _deployment-architectures:

Deployment Architectures
........................

.. rubric:: CDAP Minimal Deployment

**Note:** Minimal deployment runs all the services on single host.

.. image:: ../_images/cdap-minimal-deployment.png
   :width: 8in
   :align: center

------------

.. rubric:: CDAP High Availability and Highly Scalable Deployment

**Note:** Each component in CDAP is horizontally scalable. This diagram presents the high
availability and highly scalable deployment. The number of nodes for each component can be
changed based on the requirements.

.. image:: ../_images/cdap-ha-hs-deployment.png
   :width: 8in
   :align: center

Preparing the Cluster
---------------------
.. _install-preparing-the-cluster:

To prepare your cluster so that CDAP can write to its default namespace,
create a top-level ``/cdap`` directory in HDFS, owned by an HDFS user ``yarn``::

  $ sudo -u hdfs hadoop fs -mkdir /cdap && sudo -u hdfs hadoop fs -chown yarn /cdap

In the CDAP packages, the default HDFS namespace is ``/cdap`` and the default HDFS user is
``yarn``.

Also, create a ``tx.snapshot`` subdirectory::

  $ sudo -u hdfs hadoop fs -mkdir /cdap/tx.snapshot && sudo -u hdfs hadoop fs -chown yarn /cdap/tx.snapshot

**Note:** If you have customized ``data.tx.snapshot.dir`` in your CDAP configuration, use that value instead.

If you set up your cluster as above, no further changes are required.

.. _install-preparing-the-cluster-defaults:

If your cluster is not setup with these defaults, you'll need to 
:ref:`edit your CDAP configuration <configuration>` once you have downloaded and installed
the packages, and prior to starting services.


.. _install-packaging:

Packaging
---------
CDAP components are available as either Yum ``.rpm`` or APT ``.deb`` packages. There is
one package for each CDAP component, and each component may have multiple services.
Additionally, there is a base CDAP package with three utility packages (for HBase
compatibility) installed which creates the base configuration and the ``cdap`` user. We
provide packages for *Ubuntu 12* and *CentOS 6*.

Available packaging types:

- RPM: Yum repo
- Debian: APT repo
- Tar: For specialized installations only

**Note:** If you are using `Chef <https://www.getchef.com>`__ to install CDAP, an
`official cookbook is available <https://supermarket.getchef.com/cookbooks/cdap>`__.

Preparing Package Managers
--------------------------

.. _install-rpm-using-yum:

RPM using Yum
.............
Download the Cask Yum repo definition file:
   
.. container:: highlight

  .. parsed-literal::
    |$| sudo curl -o /etc/yum.repos.d/cask.repo |http:|//repository.cask.co/centos/6/x86_64/cdap/|short-version|/cask.repo

This will create the file ``/etc/yum.repos.d/cask.repo`` with:

.. parsed-literal::
  [cask]
  name=Cask Packages
  baseurl=http://repository.cask.co/centos/6/x86_64/cdap/|short-version|
  enabled=1
  gpgcheck=1

Add the Cask Public GPG Key to your repository:

.. container:: highlight

  .. parsed-literal::
    |$| sudo rpm --import |http:|//repository.cask.co/centos/6/x86_64/cdap/|short-version|/pubkey.gpg

Update your Yum cache::

  $ sudo yum makecache

.. end_install-rpm-using-yum

Debian using APT
................
Download the Cask APT repo definition file:

.. container:: highlight

  .. parsed-literal::
    |$| sudo curl -o /etc/apt/sources.list.d/cask.list |http:|//repository.cask.co/ubuntu/precise/amd64/cdap/|short-version|/cask.list

This will create the file ``/etc/apt/sources.list.d/cask.list`` with:

.. parsed-literal::
  deb [ arch=amd64 ] |http:|//repository.cask.co/ubuntu/precise/amd64/cdap/|short-version| precise cdap

Add the Cask Public GPG Key to your repository:

.. container:: highlight

  .. parsed-literal::
    |$| curl -s |http:|//repository.cask.co/ubuntu/precise/amd64/cdap/|short-version|/pubkey.gpg | sudo apt-key add -

Update your APT-cache::

  $ sudo apt-get update

.. end_install-debian-using-apt

Installation
------------
Install the CDAP packages by using one of these methods:

Using Chef:

  If you are using `Chef <https://www.getchef.com>`__ to install CDAP, an `official
  cookbook is available <https://supermarket.getchef.com/cookbooks/cdap>`__.

Using Yum::

  $ sudo yum install cdap-gateway cdap-kafka cdap-master cdap-security cdap-ui

Using APT::

  $ sudo apt-get install cdap-gateway cdap-kafka cdap-master cdap-security cdap-ui

Do this on each of the boxes that are being used for the CDAP components; our
recommended installation is a minimum of two boxes.

This will download and install the latest version of CDAP with all of its dependencies. 

