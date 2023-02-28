package fim

import bloomfilter.mutable.BloomFilter
import fim.Apriori.Apriori
import fim.AprioriHashTree.AprioriHashTree
import fim.Util.Itemset
import org.apache.log4j.LogManager
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

import scala.:+
import scala.collection.mutable

  object RApriori {

    val logger: org.apache.log4j.Logger = LogManager.getRootLogger;

    def main(args: Array[String]) {
      if (args.length != 2) {
        logger.error("Usage:\nfim.rApriori <input dir> <output dir>")
        System.exit(1)
      }
      val conf = new SparkConf().setAppName("FIM Apriori")
      val sc = new SparkContext(conf)
      sc.setLogLevel("WARN")

      val rApriori = new RApriori
      val frequentSets = rApriori.run(args(0), " ", Util.minSupport, sc)
      rApriori.saveFinalItemsetRDD(frequentSets, args(1), sc)
    }

    class RApriori extends AprioriHashTree {

      override def saveFinalItemsetRDD(itemsets: List[Itemset], output: String, sc: SparkContext) = {
        val rdd = sc.parallelize(itemsets).map(x => (x.size, x))
        val rdd2 = rdd.map(x => x._1).map((x) => (x, 1)).reduceByKey(_+_)
        rdd2.saveAsTextFile(output)
        println(rdd.count())
      }

      def findPairsBloomFilter(transactions: RDD[Itemset], singletons: List[String], minSupport: Int, sc: SparkContext): List[Itemset] = {
        if (singletons.nonEmpty) {
          val bf = BloomFilter[String](singletons.size, 0.01)
          singletons.foreach(bf.add)
          val bfBC = sc.broadcast(bf)
          transactions.map(t => t.filter(bfBC.value.mightContain(_)))
            .flatMap(_.combinations(2))
            .map((_, 1))
            .reduceByKey(_ + _)
            .filter(_._2 >= minSupport)
            .map(_._1.sorted)
            .collect().toList
        }
        else List.empty[Itemset]
      }

      override def findFrequentItemsets(transactions: RDD[Itemset], singletons: RDD[(String, Int)], minSupport: Int,
                                        sc: SparkContext): List[Itemset] = {

        val frequentItemsets = mutable.Map(1 -> singletons.map(_._1).map(List(_)).collect().toList)
        println(s"Number of singletons: ${frequentItemsets(1).size}")
        var k = 1
        while (frequentItemsets.get(k).nonEmpty) {
          val t0 = System.currentTimeMillis()
          k += 1

          var kFrequentItemsets = List.empty[Itemset]
          if (k == 2) {
            kFrequentItemsets = findPairsBloomFilter(transactions, frequentItemsets(1).flatten, minSupport, sc)
          }
          else {
            val candidates = generateCandidates(frequentItemsets(k - 1), sc)
            kFrequentItemsets = filterFrequentItemsets(candidates, transactions, minSupport, sc)
          }

          if (kFrequentItemsets.nonEmpty) {
            frequentItemsets.update(k, kFrequentItemsets)
            println(s"Number of itemsets with size $k: ${frequentItemsets(k).size}")
          }
          println(k, f": Elapsed time: ${(System.currentTimeMillis() - t0) / 1000d}%1.2f seconds. Class: ${getClass.getSimpleName}.")
        }
        frequentItemsets.values.flatten.toList
      }


      def run(fileName: String, separator: String, minSupport: Double, sc: SparkContext, output: String): List[Itemset] = {
        executionTime = 0
        val t0 = System.currentTimeMillis()
        val itemsets = findFrequentItemsets(fileName, separator, List.empty, minSupport, sc)
        if (executionTime == 0)
          executionTime = System.currentTimeMillis() - t0
        println(f"Elapsed time: ${executionTime / 1000d}%1.2f seconds. Class: ${getClass.getSimpleName}.")
        itemsets
      }

    }
  }