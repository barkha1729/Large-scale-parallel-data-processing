package repjoin

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.functions.broadcast


object RepJoinDatasetMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nrep.RepDMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RepJoinD")
    val sc = new SparkContext(conf)
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._

    // reading the edges file
    val textFile = sc.textFile(args(0))
    val maxValue = 50000

    // splitting and filtering with Max value
    val XtoY =
      textFile.map(line => {
        line.split(",")
      }).filter(users => users(0).toInt < maxValue && users(1).toInt < maxValue)
        .map(users => Row(users(0), users(1)))

   // creating a schema with cols
    val schema = new StructType()
      .add(StructField("X", StringType, true))
      .add(StructField("Y", StringType, true))

   // creating dataframe
    val df = sqlContext.createDataFrame(XtoY, schema)

    //  Join1 combining X1, Y1 with X2,Y2 where Y1=X2 and X1 != Y2
    val Join1 = df.select('X as "df1_X", 'Y as "df1_Y").as("XtoY")
      .join(broadcast(df.select('Y as "df2_X", 'Y as "df2_Y").as("YtoZ")),
        $"XtoY.df1_Y" === $"YtoZ.df2_X" && $"XtoY.df1_X" =!= $"YtoZ.df2_Y")

    // Join2 joining result of join1 with Z,X
    // explicitly using broadcast to specify using rep join
    val allTriangle = Join1.as("Path")
      .join(broadcast(df.as("ZtoX")),
        $"Path.df2_Y" === $"ZtoX.X" && $"Path.df1_X" === $"ZtoX.Y")

    val totalTriangleCount = allTriangle.count()/3

    // writing to the output
    println("Total Triangle Count" + totalTriangleCount )
    println(allTriangle.queryExecution.executedPlan)

    val output = sc.parallelize(Seq("Total Triangle Count: "+ totalTriangleCount))
    output.saveAsTextFile(args(1))

  }
}
