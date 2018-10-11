/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */
   
package classifier;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

/*Get words from the whole train data*/
public class Mapper1 extends MapReduceBase implements
		Mapper<LongWritable, Text, Text, IntWritable> {

	private static final String ERROR_ON_MAPPER1 = "error on mapper1:\n";

	/*
	 * input: review file.
	 * output: [word][1 or -1] (depends on the review type: positive = 1 & negative = -1)
	 */
	public void map(LongWritable key, Text value,
			OutputCollector<Text, IntWritable> output, Reporter reporter)
			throws IOException {

		String[] line;
		int review;

		try {
			String path = ((FileSplit) reporter.getInputSplit()).getPath()
					.toString();
			String[] splittedPath = (path.toString()).split("/");
			String file = splittedPath[splittedPath.length - 1];
			line = value.toString().split("[\\W]");// get words only
			review = (file.startsWith("pos")) ? 1 : -1;
			for (int i = 0; i < line.length; i++) {
				if (line[i].length() > 1) {
					output.collect(new Text(line[i].toLowerCase()),
							new IntWritable(review));
				}
			}

		} catch (Exception e) {
			System.out.println(ERROR_ON_MAPPER1 + e.getLocalizedMessage());
		}
	}
}