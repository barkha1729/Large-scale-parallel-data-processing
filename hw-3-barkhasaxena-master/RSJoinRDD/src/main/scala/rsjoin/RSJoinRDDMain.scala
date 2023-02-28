package rsjoin

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager


object RSJoinRDDMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nrsjoin.RSJoinRDDMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RSJoinRDDMain")
    val sc = new SparkContext(conf)

    val textFile = sc.textFile(args(0))
    val MAX = 62500

    // reading edges file and filtering the ids less than max
    val AB =
      textFile.map(line => {
        line.split(",")
      }).filter(userID => userID(0).toInt < MAX && userID(1).toInt < MAX)
        .map(userID => (userID(0), userID(1)))


    // creating another RDD with reversed columns so we can join on where userID2 is equal
    val BC = AB.map {
      case (userID1, userID2) => (userID2, userID1)
    }

    // joining AB and BC and creating ((C,A),B) for the next join
    val join1 = AB.join(BC).filter(ABC => {
      val AC = ABC._2
      AC._1 != AC._2
    })
      .map {
      case (userB, (userC, userA)) => ((userC, userA), userB)
    }


    // creating the last RDD to join with columns C,A
    val CA = AB.map {
      case (user1, user2) => ((user1, user2), "")
    }

    // final join to get the traingles with mathcing first and last userId
    val join2 = join1.join(CA)

    // getting counts of all traingles
    val totalTriangleCount = join2.count()

    //printing output
    println("Total Triangles with RS Join with RDD: "+ totalTriangleCount/3)

    //saving output
    val output = sc.parallelize(Seq("Total Triangles with RS Join with RDD: "+ totalTriangleCount))
    output.saveAsTextFile(args(1))
  }
}
