/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */
   
package classifier;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.lib.MultipleInputs;

/*A project used to determine if a given review is positive or negative*/
public class Driver {
	private static final String DONE = "DONE!";
	private static final String START_CLASSIFIER = "Creating the classifier, please wait...";
	private static final String START_CLASSIFICATION = "Starting the classification";
	private static final String CRC = ".crc";
	private static final String PART = "part-";
	private static final String ERROR_ON_CLASSIFICATION = "error on classification";
	private static final String ERROR_ON_CREATECLASSIFIER = "error on create classifier";
	
	/* Create classifier */
	public static void createClassifier(String trainingDataFolder,
			String classifierFolder) {
		try {
			org.apache.log4j.BasicConfigurator.configure();
			// run JOB1 (get classifier from the train data)
			System.out.println(START_CLASSIFIER);
			FileUtils.deleteDirectory(new File(classifierFolder));
			JobConf conf = new JobConf(Driver.class);
			conf.setJobName("JOB1");
			conf.setMapOutputKeyClass(Text.class);
			conf.setMapOutputValueClass(IntWritable.class);
			conf.setOutputKeyClass(Text.class);
			conf.setOutputValueClass(IntWritable.class);
			conf.setMapperClass(Mapper1.class);
			conf.setReducerClass(Reducer1.class);
			conf.setInputFormat(TextInputFormat.class);
			conf.setOutputFormat(TextOutputFormat.class);
			FileInputFormat.setInputPaths(conf, new Path(trainingDataFolder));
			FileOutputFormat.setOutputPath(conf, new Path(classifierFolder));
			JobClient.runJob(conf);
			System.out.println(DONE);
		} catch (Exception e) {
			System.out.println(ERROR_ON_CREATECLASSIFIER
					+ e.getLocalizedMessage());
		}
	}

	/* Classify a review */
	public static String runMapReduce(String inputFolder,
			String classifierFolder) throws Exception {
		System.out.println(START_CLASSIFICATION);
		// String trainF = "input";// args[0];
		String classifierF = classifierFolder;// "output";// args[1];
		String testF = inputFolder;// "input2";// args[0];
		String testResF = inputFolder.concat("res");// "output2";// args[1];
		String resultF = inputFolder.concat("final");// "output3";// args[1];
		org.apache.log4j.BasicConfigurator.configure();

		// run JOB2 (format the tested review to lines of word and the number of
		// it occurrences)
		FileUtils.deleteDirectory(new File(testResF));
		JobConf conf2 = new JobConf(Driver.class);
		conf2.setJobName("JOB2");
		conf2.setMapOutputKeyClass(Text.class);
		conf2.setMapOutputValueClass(IntWritable.class);
		conf2.setOutputKeyClass(Text.class);
		conf2.setOutputValueClass(IntWritable.class);
		conf2.setMapperClass(Mapper1.class);
		conf2.setReducerClass(Reducer1.class);
		conf2.setInputFormat(TextInputFormat.class);
		conf2.setOutputFormat(TextOutputFormat.class);
		FileInputFormat.setInputPaths(conf2, new Path(testF));
		FileOutputFormat.setOutputPath(conf2, new Path(testResF));
		JobClient.runJob(conf2);

		// run JOB3 (classify the tested review with the classifier)
		FileUtils.deleteDirectory(new File(resultF));
		JobConf conf3 = new JobConf(Driver.class);
		conf3.setJobName("JOB3");
		conf3.setMapOutputKeyClass(Text.class);
		conf3.setMapOutputValueClass(Text.class);
		conf3.setOutputKeyClass(Text.class);
		conf3.setOutputValueClass(IntWritable.class);
		MultipleInputs.addInputPath(conf3, new Path(classifierF),
				TextInputFormat.class, Mapper2_1.class);
		MultipleInputs.addInputPath(conf3, new Path(testResF),
				TextInputFormat.class, Mapper2_2.class);
		conf3.setReducerClass(Reducer2.class);
		conf3.setOutputFormat(TextOutputFormat.class);
		FileInputFormat.setInputPaths(conf3, new Path(classifierF));
		FileInputFormat.setInputPaths(conf3, new Path(testResF));
		FileOutputFormat.setOutputPath(conf3, new Path(resultF));
		JobClient.runJob(conf3);

		// read the output file
		String classification = "";
		for (final File fileEntry : new File(resultF).listFiles()) {
			if (fileEntry.toString().contains(PART)
					&& !fileEntry.toString().contains(CRC))
				try {
					classification = FileUtils.readFileToString(fileEntry);
					System.out.println(classification);
				} catch (Exception e) {
					System.out.println(ERROR_ON_CLASSIFICATION
							+ e.getLocalizedMessage());
				}
		}

		// delete temporary folders
		// FileUtils.deleteDirectory(new File(testF));
		FileUtils.deleteDirectory(new File(testResF));
		FileUtils.deleteDirectory(new File(resultF));

		System.out.println(DONE);
		// return classification result
		return classification;
	}
}