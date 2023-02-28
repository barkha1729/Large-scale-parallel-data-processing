package wc

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level

object TwitterFollowerCountMain {
  """
    |This class counts the number of followere for the user ids divisible by 100
    |It compreises of main function which reads the file from input location and
    | computes the follower counts and writes to the output location
  """

  def main(args: Array[String]) {
    """
      |Input: args comprising the the input and the output paths
      |Output: writes the counts of followers of corresponding userid in a textfile
      |"""

    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.TwitterFollowerCountMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("Twitter Follower")
    val sc = new SparkContext(conf) // creating spark Context

		// Delete output directory, only to ease local development; will not work on AWS. ===========
//    val hadoopConf = new org.apache.hadoop.conf.Configuration
//    val hdfs = org.apache.hadoop.fs.FileSystem.get(hadoopConf)
//    try { hdfs.delete(new org.apache.hadoop.fs.Path(args(1)), true) } catch { case _: Throwable => {} }
		// ================

    //reading from input
    val textFile = sc.textFile(args(0))

    //processing by filtering, mapping and then reducing the total counts
    val counts = textFile
      .map(line => {
        val userid = if (line.split(",")(1).toInt %100==0) line.split(",")(1) else None
        userid
      })
      .filter(userid => userid != None)
      .map(user=>(user,1))
      .reduceByKey(_ + _)

    logger.info(counts.toDebugString)
    counts.saveAsTextFile(args(1))

  }
}