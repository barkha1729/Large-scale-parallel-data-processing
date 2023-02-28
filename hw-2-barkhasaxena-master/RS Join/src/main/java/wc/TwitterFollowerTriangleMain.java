package wc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TwitterFollowerTriangleMain extends Configured implements Tool {
    /* This class calculates the Triangle count for the twitter edges
    * by using reduce side join and a MAX filter.
    *  There are two jobs namely:
    *  1. Path2Job: It takes edges.csv as input and creates a self join on the second id
    *                              i.e (x1,y1) and (x2,y2) yields (x1,y1,y2) for all y1=y2
    *                              This is a reduce side join as discussed in class.
    * 2. TriangleJob: It takes the output of previous job i.e path2 and joins it with edges
    *                        i.e (x,y,z) and (x2,y2) yields (x,y,z,y2) where z=x2 and y2=x.
    *                        This is a reduce side join too but with key as (z,x)
    *
    * */

    private static final Logger logger = LogManager.getLogger(TwitterFollowerTriangleMain.class);
    public static int MAX = 62500;
    /**
     * As referenced here(https://diveintodata.org/2011/03/15/an-example-of-hadoop-mapreduce-counter/), using global counter.
     */
    public enum COUNTER {
        TRIPLETRIANGLE
    }

    public static class Path2edgeMapper extends Mapper<Object, Text, Text, Text> {

        //private final static IntWritable one = new IntWritable(1);
        // private final Text word = new Text();
        private final Text userid = new Text();

        @Override
        public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException {
            // This mapper function reads the inputs line by line and parses it to
            final StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                String[] user_ids = itr.nextToken().split(",");

                if ((Integer.parseInt(user_ids[0]) < MAX) && (Integer.parseInt(user_ids[1]) < MAX)){

                    userid.set(user_ids[0]);
                    context.write(userid,new Text( user_ids[0]+","+user_ids[1]+","+"O"));

                    userid.set(user_ids[1]);
                    context.write(userid, new Text( user_ids[0]+","+user_ids[1]+","+"I"));
                }

            }

        }
    }


    public static class Path2edgeReducer extends Reducer<Text, Text, Object, Text> {

        @Override
        public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {
            // This reduce function receives the key and the list of values, sums up the values
            // for each key and write the result to the context



            List incoming = new ArrayList<>();
            List outgoing = new ArrayList<>();

            // summing up all followers for this key
            for (final Text val : values) {
                String[] tokens = val.toString().split(",");
                if (tokens[2].equals("I")){
                    incoming.add(tokens[0]);
                } else {
                    outgoing.add(tokens[1]);
                }

            }

            for ( int i = 0; i<incoming.size(); i++){
                for (int j = 0; j<outgoing.size(); j++){

                    // removing case x,y joined with y,x
                    // filtering it here will reduce some processing while forming traingles
                    if(incoming.get(i) != outgoing.get(j)) {
                        context.write(NullWritable.get(), new Text(incoming.get(i) + "," + key.toString() + "," + outgoing.get(j)));
                        //            context.getCounter(GlobalCounter.COUNT).increment(incoming.size()*outgoing.size());

                    }
                }
            }


        }
    }


    public static class Path2EdgeTriangleMapper extends Mapper<Object, Text, Text, Text> {
        /**
         * Mapper which reads the output of path2edge job and emits ((Z,X), record)
         */
        private final Text ZXKey = new Text();
        private final Text recordValue = new Text();
        @Override
        protected void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] tokens = value.toString().split(",");

            // for a record value X,Y,Z
            // making Z,X as key so that for joining (X,Y,Z) with (X2,Y2)
            // we are ensuring Z==X2 for the join , and X==Y2 for the triangle property
            ZXKey.set(new Text(tokens[2] + "," + tokens[0]));
            recordValue.set(new Text(value.toString() + "," + "path2edge"));
            context.write(ZXKey, recordValue);
        }
    }


    public static class EdgeTriangleMapper extends Mapper<Object, Text, Text, Text> {

        private final Text record = new Text();

        @Override
        protected void map(final Object key, final Text value, final Context context)
                throws IOException, InterruptedException {

            final String[] edgeToken = value.toString().split(",");
            //Max filter
            if (Integer.parseInt(edgeToken[0]) < MAX && Integer.parseInt(edgeToken[1]) < MAX) {
                record.set(new Text(value.toString() + "," + "edges"));
                context.write(value, record);
            }
        }
    }


    public static class TriangleReducer extends Reducer<Text, Text, Object, Text> {

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            // we only care about numbers, so just calculate the number of outputs
            long paths2edges = 0;
            long triangleEdges = false;

            for (Text val : values) {
                String[] tokens = val.toString().split(",");
                if (tokens.length == 3) {
                    triangleEdges=true;
                } else {
                    paths2edges++;
                }
            }

            if(triangleEdges){
                context.getCounter(COUNTER.TRIPLETRIANGLE).increment(paths2edges);
            }

        }
    }
    private int Path2Job(String input, String output)
            throws IOException, InterruptedException, ClassNotFoundException {
        final Configuration conf = getConf();
        final Job path2Job = Job.getInstance(conf, "Path2Job");
        path2Job.setJarByClass(TwitterFollowerTriangleMain.class);
        //setting the input path for this mapper class
        MultipleInputs.addInputPath(path2Job, new Path(input + "/edges.csv"),
                TextInputFormat.class, Path2edgeMapper.class);
        //setting reducer class for this job
        path2Job.setReducerClass(Path2edgeReducer.class);

        //setting key types
        path2Job.setMapOutputKeyClass(Text.class);
        path2Job.setMapOutputValueClass(Text.class);

        //setting file output
        FileOutputFormat.setOutputPath(path2Job, new Path(output + "/Temp"));
        return path2Job.waitForCompletion(true) ? 0 : 1;
    }

    private int TriangleJob(String input, String output)
            throws IOException, InterruptedException, ClassNotFoundException {
        final Configuration conf = getConf();
        final Job triangleJob = Job.getInstance(conf, "TriangleJob");
        triangleJob.setJarByClass(TwitterFollowerTriangleMain.class);

        //setting the input paths for this job
        MultipleInputs.addInputPath(triangleJob, new Path(input + "/edges.csv"),
                TextInputFormat.class, EdgeTriangleMapper.class);
        MultipleInputs.addInputPath(triangleJob, new Path(output + "/Temp"),
                TextInputFormat.class, Path2EdgeTriangleMapper.class);

        triangleJob.setReducerClass(TriangleReducer.class);

        triangleJob.setMapOutputKeyClass(Text.class);
        triangleJob.setMapOutputValueClass(Text.class);

        FileOutputFormat.setOutputPath(triangleJob, new Path(output + "/Final"));
        triangleJob.waitForCompletion(true);

        // get the counter, note that the number of counter is not the number of triangle.
        // for each triangle x, y, z, we have counted (x, y, z) , (y, z, x), (z, x, y), so the number of
        // triangle = counter / 3
        Counters cn = triangleJob.getCounters();
        Counter tripletriangle = cn.findCounter(COUNTER.TRIPLETRIANGLE);

        System.out.println(tripletriangle.getDisplayName() + ":" + tripletriangle.getValue());

        return 1;

    }


    @Override
    public int run(final String[] args) throws Exception {
        if (this.Path2Job(args[0], args[1]) == 0) {
            this.TriangleJob(args[0], args[1]);
        }
        return 0;
    }


    public static void main(final String[] args) {
        if (args.length != 2) {
            throw new Error("Two arguments required:\n<input-dir> <output-dir>");
        }

        try {
            ToolRunner.run(new TwitterFollowerTriangleMain(), args);

        } catch (final Exception e) {
            logger.error("", e);
        }
    }

}

