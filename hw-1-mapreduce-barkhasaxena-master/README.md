[![Open in Visual Studio Code](https://classroom.github.com/assets/open-in-vscode-f059dc9a6f8d3a56e377f745f24479a46679e63a5d9fe6f495e02850cd0d8118.svg)](https://classroom.github.com/online_ide?assignment_repo_id=6788571&assignment_repo_type=AssignmentRepo)
# CS6240-MR-template

Spring 2022

Code author
-----------
Barkha Saxena

Installation
------------
These components are installed:
- JDK 1.8
- Hadoop 3.3.1
- Maven
- AWS CLI (for EMR execution)

Environment
-----------
1) Example ~/.bash_aliases:
export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home
export HADOOP_HOME=/usr/local/Cellar/hadoop/3.3.1/libexec
export YARN_CONF_DIR=$HADOOP_HOME/etc/hadoop
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

2) Explicitly set JAVA_HOME in $HADOOP_HOME/etc/hadoop/hadoop-env.sh:
export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home

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
	
	download-output-aws			-- after successful execution & termination
	
