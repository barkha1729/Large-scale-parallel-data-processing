package ds

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.sum


object DSETMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.Follower_DSET <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("Follower_DSET")
    val sc = new SparkContext(conf)

    // Delete output directory, only to ease local development; will not work on AWS. ===========
    //    val hadoopConf = new org.apache.hadoop.conf.Configuration
    //    val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
    //    try { hdfs.delete(new org.apache.hadoop.fs.Path(args(1)), true) } catch { case _: Throwable => {} }
    // ================
    val sparkSession = SparkSession.builder.appName("TwitterFollowerCount").getOrCreate()
    import sparkSession.implicits._

    val countsRDD = sc.textFile(args(0))
      .map(row => row.split(",")(1))
      .filter(followedID => followedID.toInt % 100 == 0) // filter out userIDs that are divisible by 100
      .map(word => (word, 1))

    val dataSet = sparkSession.createDataset(countsRDD) // creating dataset using sparkSession and RDD
    val res = dataSet.groupBy("_1").agg(sum($"_2"))
    //performing group by on first column i.e. userId and summing corresponding value i.e. 1
    //this gives the required follower count
    res.rdd.saveAsTextFile(args(1))
  }
}
