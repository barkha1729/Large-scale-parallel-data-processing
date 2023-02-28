package fim


import fim.Apriori.Apriori
import fim.Util.Itemset
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD


object AprioriHashTree {

  class AprioriHashTree extends Apriori{

    override def filterFrequentItemsets(candidates: List[Itemset], transactionsRDD: RDD[Itemset], minSupport: Int, sc: SparkContext) = {
      if (candidates.nonEmpty) {
        val items = candidates.flatten.distinct // TODO: actually helps, or should just use singletons?
        val hashTree = new HashTree(candidates, items)
        val hashTreeBC = sc.broadcast(hashTree)
        val t1 = System.currentTimeMillis()
        val r = transactionsRDD.flatMap(t => hashTreeBC.value.findCandidatesForTransaction(t.filter(i => items.contains(i)).sorted))
          .map(candidate => (candidate, 1))
          .reduceByKey(_ + _)
          .filter(_._2 >= minSupport)
          .map(_._1)
          .collect().toList
        println(s"Searched tree in ${(System.currentTimeMillis() - t1) / 1000}s.")
        r
      }
      else List.empty[Itemset]
    }
  }
}
