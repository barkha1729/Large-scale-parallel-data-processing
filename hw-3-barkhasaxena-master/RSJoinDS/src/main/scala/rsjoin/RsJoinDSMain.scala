package rsjoin

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StringType, StructField, StructType}

object RsJoinDSMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nrs.RsDMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RS-D")
    conf.set("spark.sql.join.preferSortMergeJoin", "false")
    val sc = new SparkContext(conf)
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._



    val maxValue = 62500

    // reading file and splitting it by comma and filtering and keeping only edges with max value
    val textFile = sc.textFile(args(0))
    val XY =
      textFile.map(line => {
        line.split(",")
      }).filter(userID => userID(0).toInt < maxValue && userID(1).toInt < maxValue)
        .map(userID => Row(userID(0), userID(1)))


    // creating schema with columns (id, val)
    val schema = new StructType()
      .add(StructField("X", StringType, true))
      .add(StructField("Y", StringType, true))

   //creating dataframe
    val df = sqlContext.createDataFrame(XY, schema);

    val XY_df = df.select('X as "df1_X", 'Y as "df1_Y").as("XtoY")

    val YZ_df = df.select('X as "df2_X", 'Y as "df2_Y").as("YtoZ")

   // creating the first join on df1.Y and df2.X and removing rows where df1.X==df2.Y
    val Join1_df = XY_df.join(YZ_df)
      .where($"XtoY.df1_Y" === $"YtoZ.df2_X" && $"XtoY.df1_X" =!= $"YtoZ.df2_Y")
    
    val Join2_df = Join1_df.as("Path")
      .join(df.as("ZtoX"))
      .where($"Path.df2_Y" === $"ZtoX.X" && $"Path.df1_X" === $"ZtoX.Y")

    val totalTriangleCount = Join2_df.count()

    println("Triangle Count from RS Join with DataFrame: " + totalTriangleCount/3)
    println(Join2_df.queryExecution.executedPlan)

    // Output the counting file
    val output = sc.parallelize(Seq("Triangle Count from RS Join with DataFrame: : " + totalTriangleCount/3))
    output.saveAsTextFile(args(1))
  }
}