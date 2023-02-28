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
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TwitterFollowerCountMain extends Configured implements Tool {
    // This class calculates the total followers for user ids divisible by 100 through a
    // map reduce job reading text file from the input local and writing text file to the output
    // location.
    enum GlobalCounter {
        COUNT
    }
    private static final Logger logger = LogManager.getLogger(TwitterFollowerCountMain.class);
    public static IntWritable  path2 = new IntWritable(0);
    public static int MAX = 12500;

    public static class TokenizerMapper extends Mapper<Object, Text, Text, Text> {

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


    public static class IntSumReducer extends Reducer<Text, Text, Text, Text> {

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
                    context.write(new Text("path2edge"), new Text( incoming.get(i)+ "," + key.toString() + "," + outgoing.get(j)));
                }
            }
            context.getCounter(GlobalCounter.COUNT).increment(incoming.size()*outgoing.size());


        }
    }

    @Override
    public int run(final String[] args) throws Exception {
        final Configuration conf = getConf();
        final Job job = Job.getInstance(conf, "Twitter Follower");
        job.setJarByClass(TwitterFollowerCountMain.class); // ??
        final Configuration jobConf = job.getConfiguration();
        jobConf.set("mapreduce.output.textoutputformat.separator", "\t");
        // Delete output directory, only to ease local development; will not work on AWS. ===========
//		final FileSystem fileSystem = FileSystem.get(conf);
//		if (fileSystem.exists(new Path(args[1]))) {
//			fileSystem.delete(new Path(args[1]), true);
//		}
        // ================
        job.setMapperClass(TokenizerMapper.class);
 //       job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class); //??
        job.setOutputValueClass(Text.class); //??
        FileInputFormat.addInputPath(job, new Path(args[0])); // who is FileInputFormat??
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        int jobStatus = job.waitForCompletion(true) ? 0 : 1;
        Counter counter = job.getCounters().findCounter(GlobalCounter.COUNT);

//        // printing the total count of all length2 paths for all users
//        // output is in syslog
        System.out.println(counter.getDisplayName() + ":" +counter.getValue());

        return jobStatus;
    }

    public static void main(final String[] args) {
        if (args.length != 2) {
            throw new Error("Two arguments required:\n<input-dir> <output-dir>");
        }

        try {
                ToolRunner.run(new TwitterFollowerCountMain(), args);

        } catch (final Exception e) {
            logger.error("", e);
        }
    }

}
