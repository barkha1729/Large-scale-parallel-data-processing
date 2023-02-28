[![Open in Visual Studio Code](https://classroom.github.com/assets/open-in-vscode-f059dc9a6f8d3a56e377f745f24479a46679e63a5d9fe6f495e02850cd0d8118.svg)](https://classroom.github.com/online_ide?assignment_repo_id=6770185&assignment_repo_type=AssignmentRepo)
Frequent Itemset Mining Using Apriori Algorithm

Code authors
-----------
Shashank Shekhar
Barkha Saxena
Nikhil Anand

Overview
---------
Apriori is one of the most widely used techniques for mining Frequent itemset from a transactional database. Since apriori is very data-intensive and computing-intensive, using parallel algorithms to implement these ideas has become quite popular.
For the implementation of Apriori, we are using Spark. The reason for selecting spark is to further optimize the iterative nature of the Apriori algorithm of reading and searching the pattern in the transactional database. We will be using Spark RDDs to store the candidate keys and transactions to improve data reusability.


Installation
------------
These components are installed:
- JDK 1.8
- Scala 2.11.12
- Hadoop 2.9.1
- Spark 2.3.1 (without bundled Hadoop)
- Maven
- AWS CLI (for EMR execution)

Environment
-----------
1) Example ~/.bash_aliases:
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_311.jdk/Contents/Home
   export HADOOP_HOME=/Users/shashank/tools/hadoop-2.9.1
   export SCALA_HOME=/Users/shashank/tools/scala-2.11.12
   export SPARK_HOME=/Users/shashank/tools/spark-2.3.1-bin-without-hadoop
   export YARN_CONF_DIR=$HADOOP_HOME/etc/hadoop
   export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$SCALA_HOME/bin:$SPARK_HOME/bin
   export SPARK_DIST_CLASSPATH=$(hadoop classpath)


2) Explicitly set JAVA_HOME in $HADOOP_HOME/etc/hadoop/hadoop-env.sh:
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_311.jdk/Contents/Home

Execution
---------
All of the build & execution commands are organized in the Makefile.
1) Unzip project file.
2) Open command prompt.
3) Navigate to directory where project files unzipped.
4) Edit the Makefile to customize the environment at the top.
   Sufficient for standalone: hadoop.root, jar.name, local.input
   Other defaults acceptable for running standalone.
5) Standalone Hadoop:
   make switch-standalone		-- set standalone Hadoop environment (execute once)
   make local
6) Pseudo-Distributed Hadoop: (https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/SingleCluster.html#Pseudo-Distributed_Operation)
   make switch-pseudo			-- set pseudo-clustered Hadoop environment (execute once)
   make pseudo					-- first execution
   make pseudoq				-- later executions since namenode and datanode already running
7) AWS EMR Hadoop: (you must configure the emr.* config parameters at top of Makefile)
   make upload-input-aws		-- only before first execution
   make aws					-- check for successful execution with web interface (aws.amazon.com)
   make download-output-aws			-- after successful execution & termination
