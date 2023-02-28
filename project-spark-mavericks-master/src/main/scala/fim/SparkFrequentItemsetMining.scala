package fim

import fim.Util.{Itemset, minPartitions}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD


trait SparkFrequentItemsetMining extends FrequentItemsetMining{
  def getNumberFromPerentageSupport(minSupport: Double, numTransactions: Int) = (numTransactions * minSupport + 0.5).toInt

  def findFrequentItemsets(transactions: RDD[Itemset], singletons: RDD[(String, Int)], minSupport: Int,
                           sc: SparkContext): List[Itemset]

  /**
   * This method generates a transaction and single item RDDs and also calculates minimum support number
   * from a percentage value.
   *
   * @param fileName name of input file
   * @param separator separator used in input file
   * @param transactions transactions list (initially empty)
   * @param minSupport minimum support threshold
   * @param sc sparkContext
   * @return final frequent item sets
   */
  override def findFrequentItemsets(fileName: String, separator: String, transactions: List[Itemset], minSupport: Double, sc: SparkContext): List[Itemset] = {
    val t0 = System.currentTimeMillis()

    var transactionsRDD: RDD[Itemset] = null
    var supportCount: Int = 0

    if (!fileName.isEmpty) {
      // Fetch transaction
      //val file = List.fill(Util.replicateNTimes)(fileName).mkString(",")
      var fileRDD: RDD[String] = null
      fileRDD = sc.textFile(fileName, minPartitions)

      //getting transasctionsRDD from fileRDD

      transactionsRDD = fileRDD.filter(!_.trim.isEmpty) // filtering out empty lines
        .map(_.split(separator + "+")) // converting each transaction line into an array
        .map(l => l.map(_.trim).toList) // converting each transaction array to list


      // Using absolute
      supportCount = getNumberFromPerentageSupport(minSupport, transactionsRDD.count().toInt)
      println("supportcount: " + supportCount)
    }

    // Generate single item RDD i.e. first candidate set
    val singleItemRDD = transactionsRDD
      .flatMap(identity)
      .map(item => (item, 1))
      .reduceByKey(_ + _) // filtering duplicate value
      .filter(_._2 >= supportCount) // filtering out items with lesser supportCount value

    // calling another definition of findFrequentItemsets for further iterations
    val frequentItemsets = findFrequentItemsets(transactionsRDD, singleItemRDD, supportCount, sc)

    executionTime = System.currentTimeMillis() - t0

    //returning the final resultant frequent item sets
    frequentItemsets
  }


}
