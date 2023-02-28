package rdd

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level

object RDDFMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nrdd.RDDFMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RDDF")
    val sc = new SparkContext(conf)

    //reading the file
    val edges = sc.textFile(args(0));

    //filtering and keeping only the users being followed with userid divisible by 100
    val filteredEdges = edges.map( record => record.split(",")(1))
      .filter(followedUser => followedUser.toInt % 100 == 0);

    // adding 1 corresponding to each user being followed as a count of follower
    val mappedFollowers = filteredEdges.map(word => (word, 1));

    // adding all the followers of the filtered users using foldByKey
    val followerCount = mappedFollowers.foldByKey(0)((x,y) => x + y);

    // printing RDD lineage graph
    logger.info(followerCount.toDebugString);
    println(followerCount.toDebugString);

    // writing output (user divisible by 100, total followers of this user
    followerCount.saveAsTextFile(args(1));

  }
}

/*2022-03-04 19:44:01 INFO  root:33 - (2) ShuffledRDD[5] at foldByKey at RDDFMain.scala:30 []
 +-(2) MapPartitionsRDD[4] at map at RDDFMain.scala:27 []
    |  MapPartitionsRDD[3] at filter at RDDFMain.scala:24 []
    |  MapPartitionsRDD[2] at map at RDDFMain.scala:23 []
    |  input MapPartitionsRDD[1] at textFile at RDDFMain.scala:20 []
    |  input HadoopRDD[0] at textFile at RDDFMain.scala:20 []
(2) ShuffledRDD[5] at foldByKey at RDDFMain.scala:30 []
 +-(2) MapPartitionsRDD[4] at map at RDDFMain.scala:27 []
    |  MapPartitionsRDD[3] at filter at RDDFMain.scala:24 []
    |  MapPartitionsRDD[2] at map at RDDFMain.scala:23 []
    |  input MapPartitionsRDD[1] at textFile at RDDFMain.scala:20 []
    |  input HadoopRDD[0] at textFile at RDDFMain.scala:20 []
*/