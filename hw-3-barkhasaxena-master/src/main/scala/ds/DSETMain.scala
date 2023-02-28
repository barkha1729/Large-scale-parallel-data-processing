package ds

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level

object RDDGMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.WordCountMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("Word Count")
    val sc = new SparkContext(conf)

    // Delete output directory, only to ease local development; will not work on AWS. ===========
    //    val hadoopConf = new org.apache.hadoop.conf.Configuration
    //    val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
    //    try { hdfs.delete(new org.apache.hadoop.fs.Path(args(1)), true) } catch { case _: Throwable => {} }
    // ================

    val textFile = sc.textFile(args(0))
    val countsRDD = textFile.map( row => row.split(",")(1)) //parse a row of the RDD to get user id of followed user.
      .filter(followedID => followedID.toInt % 100 == 0) // filter out userIDs that are divisible by 100
      .map(word => (word, 1)) // for each userID divisible by 100 make a map call
      .groupByKey().mapValues(x => x.sum); //group by key and sum values for each key, this gives required follower count
    logger.info(countsRDD.toDebugString);
    //println(countsRDD.toDebugString)
    countsRDD.saveAsTextFile(args(1));
  }
}