package rdd

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level

object RDDRMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nrdd.RDDRMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RDDR")
    val sc = new SparkContext(conf)

    // Delete output directory, only to ease local development; will not work on AWS. ===========
    //    val hadoopConf = new org.apache.hadoop.conf.Configuration
    //    val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
    //    try { hdfs.delete(new org.apache.hadoop.fs.Path(args(1)), true) } catch { case _: Throwable => {} }
    // ================
    
    // reading the input edges file
    val edges = sc.textFile(args(0));

    //filtering and keeping only the users being followed with userid divisible by 100
    val filteredEdges = edges.map( record => record.split(",")(1))
      .filter(followedUser => followedUser.toInt % 100 == 0);

    // adding 1 corresponding to each user being followed as a count of follower
    val mappedFollowers = filteredEdges.map(word => (word, 1));


    // adding all the followers of the filtered users using reduceByKey
    val followerCount = mappedFollowers.reduceByKey(_ + _);

    // printing RDD lineage graph
    logger.info(followerCount.toDebugString);
    println(followerCount.toDebugString);

    // writing output (user divisible by 100, total followers of this user
    followerCount.saveAsTextFile(args(1));

  }
}

/*
* 2022-03-04 19:37:49 INFO  root:40 - (2) ShuffledRDD[5] at reduceByKey at RDDRMain.scala:37 []
 +-(2) MapPartitionsRDD[4] at map at RDDRMain.scala:33 []
    |  MapPartitionsRDD[3] at filter at RDDRMain.scala:30 []
    |  MapPartitionsRDD[2] at map at RDDRMain.scala:29 []
    |  input MapPartitionsRDD[1] at textFile at RDDRMain.scala:26 []
    |  input HadoopRDD[0] at textFile at RDDRMain.scala:26 []
(2) ShuffledRDD[5] at reduceByKey at RDDRMain.scala:37 []
 +-(2) MapPartitionsRDD[4] at map at RDDRMain.scala:33 []
    |  MapPartitionsRDD[3] at filter at RDDRMain.scala:30 []
    |  MapPartitionsRDD[2] at map at RDDRMain.scala:29 []
    |  input MapPartitionsRDD[1] at textFile at RDDRMain.scala:26 []
    |  input HadoopRDD[0] at textFile at RDDRMain.scala:26 []
*/

