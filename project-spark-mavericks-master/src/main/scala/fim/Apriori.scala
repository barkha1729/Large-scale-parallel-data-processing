package fim

import fim.Util.Itemset
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.spark.rdd.RDD

import scala.collection.mutable

object Apriori {

  val logger: org.apache.log4j.Logger  = LogManager.getRootLogger;
  //type Itemset = List[String]

  def main(args: Array[String]) {
    if (args.length != 2) {
      logger.error("Usage:\nfim.Apriori <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName(Util.appName)
    val sc = new SparkContext(conf)
    sc.setLogLevel("WARN")

    val apriori = new Apriori

    //calling run method to begin execution of apriori algorithm
    val frequentSets = apriori.run(args(0), " ", Util.minSupport, sc)

    // calling printItemsets to finally save the frequent itemsets generated in the last iteration
    apriori.saveFinalItemsetRDD(frequentSets, args(1), sc)
  }


  class Apriori extends SparkFrequentItemsetMining with Serializable {

    /**
     * Running iterations of apriori algorithm
     * @param transactions transactions RDD
     * @param singletons frequent item sets of single items
     * @param minSupport minimum support threshold
     * @param sc sparkContext
     * @return final frequent item set generated after all iterations are finished
     */
    def findFrequentItemsets(transactions: RDD[Itemset], singletons: RDD[(String, Int)], minSupport: Int,
                             sc: SparkContext): List[Itemset] = {

      //Creating a mutable map where key is size of itemset and value is list of itemsets of that size
      //Initially, mapping singleton itemsets with key '1'
      val frequentItemsets = mutable.Map(1 -> singletons.map(_._1).map(List(_)).collect().toList)
      println(s"Number of itemsets with size 1: ${frequentItemsets(1).size}")
      var k = 1
      // running iterations of apriori algorithm until we get empty set of itemsets
      while (frequentItemsets.get(k).nonEmpty) {
        k += 1
        // Generating candidate set for next iteration and performing pruning
        val candidates = generateCandidates(frequentItemsets(k - 1), sc)
        // Comparing frequent itemsets with transactionsRDD, and filtering out itemsets
        // with frequency less than minimum support threshold
        val kFrequentItemsets = filterFrequentItemsets(candidates, transactions, minSupport, sc)
        if (kFrequentItemsets.nonEmpty) {
          frequentItemsets.update(k, kFrequentItemsets)
          println(s"Number of itemsets with size $k: ${frequentItemsets(k).size}")
        }
      }
      frequentItemsets.values.flatten.toList
    }

    /**
     * Generating candidate itemsets for each iteration and performing pruning
     * @param frequentItemSets frequent itemset of current iteration
     * @param sc spark context
     * @return pruned frequent itemset set for next iteration.
     */
    def generateCandidates(frequentItemSets: List[Itemset], sc: SparkContext)= {
      // converting current itemsets in frequentItemSets list to RDD
      val previousFrequentSets = sc.parallelize(frequentItemSets)

      // performing self join (cartesian product) to generate candidate itemset
      val cartesian = previousFrequentSets.cartesian(previousFrequentSets).filter {
        case (a, b) => a.mkString("") > b.mkString("")
      }

      cartesian
        .flatMap({ case (a, b) =>
          var result: List[Itemset] = null
          if (a.size == 1 || allElementsEqualButLast(a, b)) {
            val newItemset = (a :+ b.last).sorted
            // Pruning all itemsets from candidate itemsets that are infrequent
            if (pruneItemsets(newItemset, frequentItemSets))
              result = List(newItemset)
          }
          if (result == null) List.empty[Itemset] else result
        })
        .collect().toList
    }

    /**
     * Helper method to check if all elements except last are equal in two strings
     * @param a first string
     * @param b second string
     * @return true if all elements except last are equal in two strings, otherwise false
     */
    def allElementsEqualButLast(a: List[String], b: List[String]): Boolean = {
      for (i <- 0 until a.size - 1) {
        if (a(i) != b(i))
          return false
      }
      if (a.last == b.last) {
        return false
      }
      true
    }

    /**
     * Pruning itemsets by comparing subsets of previous iteration
     * @param itemset newly generated itemset
     * @param previousItemsets list of previous itemsets
     * @return true if itemset is valid, otherwise false
     */
    def pruneItemsets(itemset: List[String], previousItemsets: List[Itemset]): Boolean = {
      for (i <- itemset.indices) {
        val subset = itemset.diff(List(itemset(i)))
        val found = previousItemsets.contains(subset)
        if (!found) {
          return false
        }
      }
      true
    }

    /**
     * Filtering frequent itemsets by comparing them with transactionsRDD
     * @param candidates candidate itemset
     * @param transactionsRDD transactions RDD
     * @param minSupport minimum support threshold
     * @param sc sparkContext
     * @return frequent itemset
     */

    def filterFrequentItemsets(candidates: List[Itemset], transactionsRDD: RDD[Itemset], minSupport: Int, sc: SparkContext) = {
      // broadcasting candidate itemset
      val candidatesBroadCast = sc.broadcast(candidates)

      // checking if all items of a candidate itemset are present in the same transaction
      val filteredCandidatesRDD = transactionsRDD.flatMap(t => {
        candidatesBroadCast.value.flatMap(c => {
          if (candidateExistsInTransaction(c, t))
            List(c)
          else
            List.empty[Itemset]
        })
      })

      // checking if frequency of itemset in transactionsRDD is greater than the minimum support threshold
      filteredCandidatesRDD.map((_, 1))
        .reduceByKey(_ + _)
        .filter(_._2 >= minSupport)
        .map(_._1)
        .collect().toList
    }


    /**
     * Helper method to check if elements of candidate itemsets exist in transaction
     * @param candidate candidate itemset
     * @param transaction one transasction itemset
     * @return true if candidate exists in transaction, otherwise false
     */
    def candidateExistsInTransaction(candidate: Itemset, transaction: Itemset): Boolean = {
      var result = true
      for (elem <- candidate) {
        if (!transaction.contains(elem))
          result = false
      }
      result
    }


    /**
     * Saving final itemsets RDD that results from the last iteration of apriori algorithm
     * @param itemsets List of final itemsets that came from last iteration
     * @param output Path where we store final RDD
     * @param sc sparkContext
     */
    def saveFinalItemsetRDD(itemsets: List[Itemset], output: String, sc: SparkContext) = {
      val rdd = sc.parallelize(itemsets).map(x => (x.size, x))
      rdd.saveAsTextFile(output)
      println(rdd.count())
    }
  }

}