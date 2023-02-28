package pr

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager

import org.apache.spark.rdd.RDD
import scala.collection.mutable.ListBuffer

object PageRankMain {

  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length == 4) {
      println("The input arguments: ")
      println("k = " + args(0))
      println("#Iteration = " + args(1))
      println("Input dir = " + args(2))
      println("Output dir = " + args(3))
    }
    else {
      logger.error("Usage:\npr.PageRankMain <k> <number of iterations> <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("Page Rank")
    val sc = new SparkContext(conf)

    // parsing the command line arguments
    //setting value of k and hence number of nodes
    val k = args(0).toInt
    val total_vertices = k * k

    // setting number of iterations here
    val iterations = args(1).toInt

    val output = args(3)

    // To store the pagerank of each node, creating a list buffer
    val nodePRList = new ListBuffer[(Int, Double)]()

    // Appending the dummy page i.e node 0 with 0.0 PR
    nodePRList.append((0, 0.0))

    // At the starting, we assuming each page has 1/N
    val initialPageRank = 1.0 / total_vertices

    // To store the edges (i,j) where i points to j,  creating a list buffer
    val graphOutgoingEdgeList = new ListBuffer[(Int, Int)]()
    for (vertex_no <- 1 to total_vertices) {
      if (vertex_no % k == 0) {
        // dangling page
        graphOutgoingEdgeList.append((vertex_no, 0))
      } else {
        // normal page edge
        graphOutgoingEdgeList.append((vertex_no, vertex_no + 1))
      }
      nodePRList.append((vertex_no, initialPageRank))
    }

    // Creating RDDs
    var ranks: RDD[(Int, Double)] = sc.parallelize(nodePRList.toList)
    val graph: RDD[(Int, Int)] = sc.parallelize(graphOutgoingEdgeList.toList)

    var graphPageRank = sc.emptyRDD[(Int, Double)]


    for (i <- 1 to iterations) {

      graphPageRank = graph.join(ranks).map(tuple => (tuple._2._1, tuple._2._2))
      //printing after each iteration
//      println("Iteration #"+i)
      // graphPageRank.collect().foreach(println)

      // aggregation for dummy node as all others will have only one value
      var intermediatePR = graphPageRank.reduceByKey(_ + _)

      // calculating the dummyPR by getting the PR value after aggregation from dummy node
      val dummyPR = intermediatePR.lookup(0).head / total_vertices

      // updating Page ranks for all nodes with the formula and zero for dummy as per the algorithm
      intermediatePR = intermediatePR.map(tuple => {

        if (tuple._1 == 0) {
          (tuple._1, 0) //assigning zero to dummy for next iteration
        } else {
          (tuple._1, 0.15 * 1.0 / total_vertices + 0.85 * (tuple._2 + dummyPR)) //updating for other vertex
        }
      })

      //  Updating PR for nodes with input from dangling

      // For faster extraction
      val dict: collection.Map[Int, Double] = intermediatePR.collectAsMap()

      // final rank
      ranks = ranks.map(tuple => {
        if (dict.contains(tuple._1)) {
          (tuple._1, dict.get(tuple._1).head)
        } else {
          // adding to only those nodes which have input from dangling
          (tuple._1, 0.15 * 1.0 / total_vertices + 0.85 * dummyPR)
        }
      })

//      ranks.cache()


    }
    logger.info(ranks.toDebugString)
    ranks.filter(tuple => {tuple._1 <= 1000})saveAsTextFile(output)

  }

}