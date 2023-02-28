package rdd

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level

object RDDAMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.WordCountMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("Word Count")
    val sc = new SparkContext(conf)

    //reading the file
    val edges = sc.textFile(args(0));

    //filtering and keeping only the users being followed with userid divisible by 100
    val filteredEdges = edges.map( record => record.split(",")(1))
      .filter(followedUser => followedUser.toInt % 100 == 0);

    // adding 1 corresponding to each user being followed as a count of follower
    val mappedFollowers = filteredEdges.map(word => (word, 1));

    // adding all the followers of the filtered users using aggregateByKey
    // with zeroValue as 0
    // along with summation 1. within the partitions and 2. across the partitions
    val followerCount = mappedFollowers.aggregateByKey(0)((x, y) => x + y, (u1, u2) => u1 + u2);

    // printing RDD lineage graph
    logger.info(followerCount.toDebugString);
    println(followerCount.toDebugString);

    // writing output (user divisible by 100, total followers of this user
    followerCount.saveAsTextFile(args(1));

  }
}

/*
* 2022-03-04 19:49:34 INFO  root:35 - (2) ShuffledRDD[5] at aggregateByKey at RDDAMain.scala:32 []
 +-(2) MapPartitionsRDD[4] at map at RDDAMain.scala:27 []
    |  MapPartitionsRDD[3] at filter at RDDAMain.scala:24 []
    |  MapPartitionsRDD[2] at map at RDDAMain.scala:23 []
    |  input MapPartitionsRDD[1] at textFile at RDDAMain.scala:20 []
    |  input HadoopRDD[0] at textFile at RDDAMain.scala:20 []
(2) ShuffledRDD[5] at aggregateByKey at RDDAMain.scala:32 []
 +-(2) MapPartitionsRDD[4] at map at RDDAMain.scala:27 []
    |  MapPartitionsRDD[3] at filter at RDDAMain.scala:24 []
    |  MapPartitionsRDD[2] at map at RDDAMain.scala:23 []
    |  input MapPartitionsRDD[1] at textFile at RDDAMain.scala:20 []
    |  input HadoopRDD[0] at textFile at RDDAMain.scala:20 []
*/