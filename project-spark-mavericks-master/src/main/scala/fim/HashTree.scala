package fim

import fim.Apriori.Apriori
import fim.Util.Itemset

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object HashTree {
  def main(args: Array[String]): Unit = {
    val c = "1,4,5; 1,2,4; 4,5,7; 1,2,5; 4,5,8; 1,5,9; 1,3,6; 2,3,4; 5,6,7; 3,4,5; 3,5,6; 3,5,7; 6,8,9; 3,6,7; 3,6,8".replaceAll(";", "\n")
    //val c = "1,5; 1,3; 1,7".replaceAll(";", "\n")
    val candidates = Util.parseTransactionsByText(c)
    val hashTree = new HashTree(candidates, List())
    val subsets = hashTree.findCandidatesForTransaction(List("1", "2", "3", "5", "6"))
    println("\nFound subsets: " + subsets)
  }
}

class HashTree(val candidates: List[Itemset], val items: List[String]) extends Serializable {

  val size = candidates.head.size
  val rootNode = new Node(0)

  val t0 = System.currentTimeMillis()
  // TODO: would group by ratio help?
  val hashes = mutable.Map[String, Int]()
  for (i <- items.indices) {
    hashes.update(items(i), i % Math.max(size, items.size / size))
    //hashes.update(items(i), i % size) alternative implementation
  }

  for (candidate <- candidates) {
    putCandidate(candidate, rootNode)
  }
  println(s"Built tree of size $size and rows ${candidates.size} in ${(System.currentTimeMillis() - t0) / 1000}s.")

  private def putCandidate(candidate: Itemset, currentNode: Node): Unit = {
    val nextNode = getNextNode(candidate, currentNode)
    if (nextNode.children.nonEmpty) {
      // go to the next tree level
      putCandidate(candidate, nextNode)
    }
    else if (nextNode.candidatesBucket.size < size || currentNode.level >= size - 1) {
      // put into the bucket
      nextNode.candidatesBucket.append(candidate)
    }
    else {
      // bucket size exceeded, break into children nodes
      for (elem <- nextNode.candidatesBucket) {
        putCandidate(elem, nextNode)
      }
      putCandidate(candidate, nextNode)
      nextNode.candidatesBucket.clear()
    }
  }

  private def getNextNode(candidate: Itemset, currentNode: Node): Node = {
    val item = candidate(currentNode.level)
    val position = hash(item)
    if (currentNode.children.contains(position)) {
      currentNode.children(position)
    }
    else {
      val node = new Node(currentNode.level + 1)
      currentNode.children.update(position, node)
      node
    }
  }

  def findCandidatesForTransaction(transaction: Itemset): ListBuffer[Itemset] = {
    findCandidatesForTransaction(transaction, 0, rootNode)
      .distinct
      .flatMap(_.candidatesBucket)
      .filter(apriori.candidateExistsInTransaction(_, transaction))
  }

  private def findCandidatesForTransaction(transaction: Itemset, start: Int, currentNode: Node): ListBuffer[Node] = {
    val foundCandidates = new ListBuffer[Node]()

    for (i <- start until Math.min(transaction.size, transaction.size - size + currentNode.level + 1)) {
      val item = transaction(i)
      val nextNode = findNextNode(item, currentNode)
      if (nextNode != null) { // node may not exist for a given transaction
        if (nextNode.candidatesBucket.nonEmpty) {
          foundCandidates.append(nextNode)
        }
        else {
          val nextCandidates = findCandidatesForTransaction(transaction, i + 1, nextNode)
          foundCandidates.appendAll(nextCandidates)
        }
      }
    }
    foundCandidates
  }

  val apriori = new Apriori()

  def findNextNode(item: String, currentNode: Node): Node = {
    val position = hash(item)
    if (currentNode.children.contains(position))
      currentNode.children(position)
    else
      null
  }

  def hash(item: String): Int = hashes(item)

  class Node(val level: Int) extends Serializable {

    val children = mutable.Map[Int, Node]()
    val candidatesBucket = new ListBuffer[Itemset]()

  }

}
