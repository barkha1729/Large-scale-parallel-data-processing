package PR;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class PageRankMapReduce extends Configured implements Tool {
  private static long doubleToLong = Long.parseLong("1000000000000000");
  private static final Logger logger = LogManager.getLogger(PageRankMapReduce.class);
  enum GlobalCounter {
    pagerank_sum,
    delta
  }

  /**
   * Main Mapper class for PageRank algorithm.
   */
  public static class PageRankMapper extends Mapper<Object, Text, IntWritable, CustomNode> {

    @Override
    public void map(final Object key, final Text graphNode,
                    final Context context) throws IOException, InterruptedException {

      double delta = Double.parseDouble(context.getConfiguration().get("delta"));
      double totalGraphNodes = Integer.parseInt(context.getConfiguration().get("totalGraphNodes"));

      CustomNode customNode = new CustomNode(graphNode);

      //updating nodePR by adding delta factor to it
      double updatedPR = customNode.getNodePR() + 0.85 * delta / totalGraphNodes;
      //^ assuming alpha is 0.85
      customNode.updatePageRank(updatedPR);


      // pass along the graph structure, emit (N.id, N)
      context.write(new IntWritable(customNode.getVertexId()), customNode);
      String[] connectedNodes = customNode.getConnectedNodes();

      double dividePR = updatedPR/connectedNodes.length;

      for(String connectedNode: connectedNodes) {
        if(!connectedNode.isEmpty()) {
          int connectedNodeId = Integer.parseInt(connectedNode);
          //sending a part of new pagerank to all connceted nodes
          context.write(new IntWritable(connectedNodeId),
                  new CustomNode(-1, "", -1, dividePR));
        }
      }
    }
  }


  /**
   * Main Reducer class for PageRank algorithm.
   */
  public static class PageRankReducer extends Reducer<IntWritable, CustomNode, IntWritable, CustomNode> {

    @Override
    public void reduce(final IntWritable key, final Iterable<CustomNode> values, final Context context)
            throws IOException, InterruptedException {
      double totalGraphNodes = Integer.parseInt(context.getConfiguration().get("totalGraphNodes"));
      CustomNode customNode = new CustomNode();
      int id = key.get();
      double inlinkPRContrb = 0.0;

      for(CustomNode value: values) {
        if(value.getVertexId() != -1) {
          // Recovering graph structure
          customNode = new CustomNode(value.getVertexId(), value.getAdjacencyList(),value.getNodePR(), -1);
        }
        else {
          // A PageRank contribution from an inlink was found: // add it to the running sum
          inlinkPRContrb += value.getNodePRContribution();
        }
      }

      // finding updated pageRank
      double updatedPageRank = 0.0;
      if (id!=0) { // not dummy
        updatedPageRank = 0.15 * (1.0/totalGraphNodes) + 0.85 * (inlinkPRContrb);
      }

      customNode.updatePageRank(updatedPageRank);
      context.write(key, customNode);

      // Reset delta in global counter if currentCustomNode is dummy
      if (customNode.getVertexId() == 0) {
        context.getCounter(GlobalCounter.delta).setValue((long) (inlinkPRContrb * doubleToLong));
      }
    }
  }

  /***
   * Final mapper used to add delta contribution in final PR values.
   */
  public static class FinalPageRankMapper extends Mapper<Object, Text, IntWritable, CustomNode> {
    double delta;
    int totalGraphNodes;

    @Override
    public void setup(Context context) {
      delta = Double.parseDouble(context.getConfiguration().get("delta"));
      totalGraphNodes = Integer.parseInt(context.getConfiguration().get("totalGraphNodes"));
    }

    @Override
    public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException {
      CustomNode customNode = new CustomNode(value);
      double pageRank = 0.0;


      if(customNode.getVertexId()!=0) {
        //updating nodePR by adding delta factor to it
        pageRank = customNode.getNodePR() + 0.85 * delta / totalGraphNodes;
      }

      customNode.updatePageRank(pageRank);
      context.write(new IntWritable(customNode.getVertexId()), customNode);

      // Getting final pagerank sum to see if it is close to 1.
      // Significant difference from 1  indicates program has error.
      context.getCounter(GlobalCounter.pagerank_sum).increment((long) (pageRank * doubleToLong));
    }
  }






  @Override
  public int run(final String[] args) throws Exception {
    final int k = Integer.parseInt(args[2]);
    final int noOfIterations = Integer.parseInt(args[3]);
    final int totalGraphNodes = k * k;

    //Creating inputFile to write generated graph into it
    File inputFile = new File("input","input.txt");

    // Creating inputDir graph of the form (Node, adjacencyList(Node), PageRank (Node)
    StringBuilder inputGraph = new StringBuilder();
    for (int i = 1; i <= k * k; i++) {
      inputGraph.append( ( i % k == 0) ? i + ","  + "0" + "," + (1.0/totalGraphNodes)
              : i + "," + (i + 1) + "," + (1.0/totalGraphNodes));
      inputGraph.append("\n");
    }
    // adding dummy node to graph
    String dummyNode = "0,,0";
    inputGraph.append(dummyNode);


    FileWriter writer = new FileWriter(inputFile);
    writer.write(inputGraph.toString());
    writer.close();

    Path inputDir = new Path(args[0]);
    Path outputDir = new Path(args[1]);
    outputDir = new Path(outputDir, "0");


    // Separately running for job 0 with delta 0
    Job job = createJob(0, 0.0, String.valueOf(totalGraphNodes), inputDir, k);
    FileInputFormat.setInputPaths(job, inputDir);
    FileOutputFormat.setOutputPath(job, outputDir);


    job.setMapperClass(PageRankMapper.class);
    job.setReducerClass(PageRankReducer.class);

    if(!job.waitForCompletion(true))
      return -1;

    for (int i = 1; i <= noOfIterations; i++) {
      inputDir = outputDir;
      outputDir = new Path(outputDir, String.valueOf(i));
      double delta = (double) job.getCounters().
              findCounter(GlobalCounter.delta).getValue() / doubleToLong;

      // fetching delta from global counter and sending it into next iteration
      job = createJob(i, delta, String.valueOf(totalGraphNodes), inputDir, k);

      FileInputFormat.setInputPaths(job, inputDir);
      FileOutputFormat.setOutputPath(job, outputDir);
      job.setMapperClass(PageRankMapper.class);
      job.setReducerClass(PageRankReducer.class);

      if (!job.waitForCompletion(true))
        return -1;
    }
    inputDir = outputDir;
    outputDir = new Path(outputDir, "finalDir");


// Running last job after final iteration to fix PR by adding delta values calculated in kth iteration
    double delta = (double)job.getCounters().findCounter(GlobalCounter.delta).getValue()/ doubleToLong;
    job = createJob(noOfIterations + 1, delta, totalGraphNodes + "", inputDir, k);

    FileInputFormat.setInputPaths(job, inputDir);
    FileOutputFormat.setOutputPath(job, outputDir);


//Running FinalPageRankMapper for final job to add delta calculated in kth iteration
    job.setMapperClass(FinalPageRankMapper.class);
    job.setNumReduceTasks(0);


    int ret = job.waitForCompletion(true) ? 0:1;

    logger.info("PR sum = " +
            (double)(job.getCounters().findCounter(GlobalCounter.pagerank_sum).getValue())/ doubleToLong);

    return ret;
  }

  // method to create a generic job with all the parameters.
  private Job createJob(int iteration, double delta, String totalGraphNodes, Path inputPath, int k) throws IOException {
    final Configuration conf = getConf();
    final Job job = Job.getInstance(conf, "PageRankMapReduce" + iteration);
    job.setJarByClass(PageRankMapReduce.class);

    final Configuration jobConf = job.getConfiguration();

    jobConf.set("mapreduce.output.textoutputformat.separator", ",");

    //Passing delta and totalGraphNodes in context
    job.getConfiguration().set("delta", String.valueOf(delta));
    job.getConfiguration().set("totalGraphNodes", totalGraphNodes);

    // Using NLineInputFormat as suggested in the assignment
    job.setInputFormatClass(NLineInputFormat.class);
    NLineInputFormat.addInputPath(job, inputPath);
    job.getConfiguration().setInt("mapreduce.input.lineinputformat.linespermap",
            Integer.parseInt(totalGraphNodes) / 20);
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(CustomNode.class);
    return job;
  }

  public static void main(final String[] args) {
    if (args.length != 4) {
      throw new Error("Four arguments required:\n<input-dir> <output-dir> <k> <iterations>");
    }

    try {
      ToolRunner.run(new PageRankMapReduce(), args);
    } catch (final Exception e) {
      logger.error("", e);
    }
  }
}