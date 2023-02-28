package wc;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
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
    private static final Logger logger = LogManager.getLogger(TwitterFollowerCountMain.class);

    public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {

        private final static IntWritable one = new IntWritable(1);
        private final Text word = new Text();

        @Override
        public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException {
            // This mapper function reads the inputs line by line and parses it to
            //   write (userid, one) for all userids divisble by 100
            final StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                String user_id = itr.nextToken().split(",")[1];
                Integer id = Integer.parseInt(user_id);
                //checking divisiblity by 100 and writing for only user ids divisible by 100
                if (id%100 == 0){
                    word.set(Integer.toString(id));
                    context.write(word, one);
                }

            }
        }
    }

    public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private final IntWritable result = new IntWritable();

        @Override
        public void reduce(final Text key, final Iterable<IntWritable> values, final Context context) throws IOException, InterruptedException {
            // This reduce function receives the key and the list of values, sums up the values
            // for each key and write the result to the context
            int sum = 0;
            // summing up all followers for this key
            for (final IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
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
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class); //??
        job.setOutputValueClass(IntWritable.class); //??
        FileInputFormat.addInputPath(job, new Path(args[0])); // who is FileInputFormat??
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        return job.waitForCompletion(true) ? 0 : 1;
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