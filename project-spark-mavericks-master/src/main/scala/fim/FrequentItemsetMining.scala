package fim

import fim.Util.Itemset
import org.apache.spark.SparkContext

trait FrequentItemsetMining{

  // to calculate executionTime
  var executionTime: Long = 0

  def findFrequentItemsets(fileName: String = "", separator: String = "", transactions: List[Itemset], minSupport: Double, sc: SparkContext): List[Itemset]

  def run(transactions: List[Itemset], minSupport: Double, sc: SparkContext): List[Itemset] = {
    executionTime = 0
    val t0 = System.currentTimeMillis()
    val itemsets = findFrequentItemsets("", "", transactions, minSupport, sc)
    if (executionTime == 0)
      executionTime = System.currentTimeMillis() - t0
    println(f"Elapsed time: ${executionTime / 1000d}%1.2f seconds. Class: ${getClass.getSimpleName}. Items: ${transactions.size}")
    itemsets
  }

  /**
   * Run method calls findFrequentItemsets to read input file and to measure time of execution
   * @param fileName input file
   * @param separator separator used in input file that will further be used to parse it
   * @param minSupport minimum support threshold value
   * @param sc spark context
   * @return final itemsets generated in last iteration
   */

  def run(fileName: String, separator: String, minSupport: Double, sc: SparkContext): List[Itemset] = {
    executionTime = 0
    val t0 = System.currentTimeMillis()
    // calling find findFrequentItemsets to begin the first iteration
    val itemsets = findFrequentItemsets(fileName, separator, List.empty, minSupport, sc)
    if (executionTime == 0)
      executionTime = System.currentTimeMillis() - t0
    println(f"Elapsed time: ${executionTime / 1000d}%1.2f seconds. Class: ${getClass.getSimpleName}.")
    itemsets
  }
}
