package repjoin

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager

object RepJoinRDDMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nrep.RepRMain <input dir> <output dir>")
      System.exit(1)
    }

    //spark config
    val conf = new SparkConf().setAppName("RDD Replicated join")
    val sc = new SparkContext(conf)
    val totalTriangleCount = sc.longAccumulator;

    //reading text file
    val textFile = sc.textFile(args(0))

    //splitting users by comma and filtering
    val maxValue = 50000
    val userFollower =
      textFile.map(line => {
        line.split(",")
      }).filter(userid => userid(0).toInt < maxValue && userid(1).toInt < maxValue)
        .map(userid => (userid(0).toInt, userid(1).toInt))


    // aggregating all followed users by using reduce by key
    // resulting in a structure like (follower, Set( followed1, followed2, followed3,....)
    val followingUser = userFollower.map(rdd => (rdd._1, Set(rdd._2)))
      .reduceByKey(_ ++ _)


    //broadcasting the rdd as a map
    val broadcastRdd = sc.broadcast(followingUser.collect.toMap)


    // for all pairs (x,y) get all the z that y follows then for every z fetch all the x that z follows but they are not x
    // check if x is present in z's list and increment counter by 1 if all conditions are met
    val allTriangleCount = userFollower.map {
      case (userX, userY) => broadcastRdd.value.getOrElse(userY, Set[Int]()).foreach {
        userZ => if(userZ != userX && broadcastRdd.value.getOrElse(userZ, Set[Int]()).contains(userX)) {
          totalTriangleCount.add(1)
        }
      }
    }

    // lazy evaluation of the transformation being executed at this count action call
    allTriangleCount.collect()

    val triangleCount = totalTriangleCount.value/3

    logger.info("The total number of triangles with Rep Join (Max=50000) with RDD:  " +triangleCount )

    //writing output
    val output = sc.parallelize(Seq("The total number of triangles with Rep Join (Max=50000) with RDD : "+ triangleCount))
    output.saveAsTextFile(args(1))
  }
}