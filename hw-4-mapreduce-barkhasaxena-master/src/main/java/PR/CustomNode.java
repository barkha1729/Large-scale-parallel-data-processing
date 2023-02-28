package PR;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * Custom graph node that has nodeId, node adjacency list, node pagerank and node PRContribution
 * to save their id, adjacencylist, pagerank, and to be distributed PageRank
 */
public class CustomNode implements Writable {

  private int nodeId = -1;
  private String adjList = "";
  private double nodePR = -1;
  private double nodePRContribution = -1;

  // default constructor used in reducer
  CustomNode() {
  }

    CustomNode(int nodeId, String adjList, double nodePR, double distribute){
      this.nodeId = nodeId;
      this.adjList = adjList;
      this.nodePR = nodePR;
      this.nodePRContribution = distribute;
    }

    CustomNode(Text inputNode) {
      String graphNode = inputNode.toString();
      setVertexId(graphNode);
      setAdjacencyList(graphNode);
      setNodePR(graphNode);
      this.nodePRContribution = -1;
    }

    public void setVertexId (String node){
      String[] entities = node.split(",");

      int id = Integer.parseInt(entities[0]);
      this.nodeId = id;
    }

    public String getAdjacencyList () {
      return this.adjList;
    }

    public void setAdjacencyList(String node){
      String[] entities = node.split(",");

      String list = entities[1];
      this.adjList = list;
    }

    public int getVertexId () {
      return this.nodeId;
    }

    public double getNodePR() {
      return this.nodePR;
    }

    public void setNodePR(String node){
      String[] entities = node.split(",");
      double pr = Double.parseDouble(entities[2]);
      this.nodePR = pr;
    }

    public void updatePageRank ( double newPagerank) {
      this.nodePR = newPagerank;
    }

    public double getNodePRContribution() {
      return this.nodePRContribution;
    }

    public String[] getConnectedNodes () {
      return this.adjList.split(",");
    }

    @Override
    public void write (DataOutput dataOutput) throws IOException {
      dataOutput.writeInt(nodeId);
      dataOutput.writeDouble(nodePR);
      dataOutput.writeDouble(nodePRContribution);
      dataOutput.writeUTF(adjList);
    }

    @Override
    public void readFields (DataInput dataInput) throws IOException {
      this.nodeId = dataInput.readInt();
      this.nodePR = dataInput.readDouble();
      this.nodePRContribution = dataInput.readDouble();
      this.adjList = dataInput.readUTF();
    }

    @Override
    public String toString () {
      return this.adjList + "," + String.format("%2.12f", this.nodePR);
    }
}
