package hipi.experiments.iris;

import hipi.experiments.mapreduce.JPEGFileInputFormat;
import hipi.experiments.mapreduce.JPEGSequenceFileInputFormat;
import hipi.image.FloatImage;
import hipi.image.ImageHeader;
import hipi.image.io.ImageEncoder;
import hipi.image.io.JPEGImageUtil;
import hipi.imagebundle.mapreduce.ImageBundleInputFormat;
import java.util.Date;
import hipi.util.ByteUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.apache.hadoop.io.BooleanWritable;

public class iris extends Configured implements Tool{

	public static class MyMapper extends Mapper<ImageHeader, FloatImage, BooleanWritable, DoubleWritable>
	{
		private Path path;
		private FileSystem fileSystem;
		private Configuration conf;
		
		public void setup(Context jc) throws IOException
		{
			conf = jc.getConfiguration();
			fileSystem = FileSystem.get(conf);
			path = new Path( conf.get("iris.outdir"));
			fileSystem.mkdirs(path);
		}
		
		public void map(ImageHeader key, FloatImage value, Context context) throws IOException, InterruptedException
		{
			if (value != null) 
			{
				FileInputStream file = new FileInputStream(conf.get("iris.filename"));
				FloatImage inputimage = JPEGImageUtil.getInstance().decodeImage(file);
				float input[]=inputimage.getData();
				float input2[]=value.getData();
				double d = 0;
	            		for (int i = 0; i < input.length; i++)
	                    		d += Math.pow(input[i] - input2[i], 2);
	            		System.out.println("Distance :- "+d);
		
	            		context.write(new BooleanWritable(true), new DoubleWritable(d));
			}
			else
				context.write(new BooleanWritable(false), new DoubleWritable(0));
		}
	}

	
	public int run(String[] args) throws Exception
	{	
			// Read in the configurations
			if (args.length < 3)
			{
				System.out.println("Usage: im2gray <inputdir> <outputdir> <file to search>");
				System.exit(0);
			}

			// Setup configuration
			Configuration conf = new Configuration();
			// set the dir to output the jpegs to
			String outputPath = args[1];
			String filename = args[2];
			conf.setStrings("iris.outdir", outputPath);
			conf.setStrings("iris.filename", filename);

			Job job = new Job(conf, "iris");
			job.setJarByClass(iris.class);
			job.setMapperClass(MyMapper.class);
			job.setReducerClass(Reducer.class);

			// Set formats
			job.setOutputKeyClass(BooleanWritable.class);
			job.setOutputValueClass(DoubleWritable.class);       

			FileOutputFormat.setOutputPath(job, new Path(outputPath));
			JPEGFileInputFormat.addInputPath(job, new Path(args[0]));
			job.setInputFormatClass(ImageBundleInputFormat.class);
			//conf.set("mapred.job.tracker", "local");
			job.setNumReduceTasks(0);
			long lStartTime = new Date().getTime();
			if(job.waitForCompletion(true))
			{
				long lEndTime = new Date().getTime();
				long difference = lEndTime - lStartTime;
				System.out.println("Elapsed milliseconds: " + difference);
				System.exit(0);
			}
			else
			{
				long lEndTime = new Date().getTime();
				long difference = lEndTime - lStartTime;
				System.out.println("Elapsed milliseconds: " + difference);
				System.exit(1);	
			}
			return 0;
		}
	
	    	public static void main(String[] args) throws Exception {
			int res = ToolRunner.run(new iris(), args);
			System.exit(res);
		}
	}
