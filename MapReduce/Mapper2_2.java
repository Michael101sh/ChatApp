/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */
   
package classifier;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

/*Get test (set of all words and occurrences) with prefix*/
public class Mapper2_2 extends MapReduceBase implements
		Mapper<LongWritable, Text, Text, Text> {

	private static final String KEY = "key";
	private static final String PREFIX_B = "test ";
	private static final String ERROR_ON_MAPPER2_2 = "error on mapper2_2:\n";

	/*
	 * input: [word][number of it occurrences]
	 * output: ["key"]["test " + word + number of it occurrences]
	 */
	public void map(LongWritable key, Text value,
			OutputCollector<Text, Text> output, Reporter reporter)
			throws IOException {
		try {
			output.collect(new Text(KEY), new Text(PREFIX_B + value.toString()));
		} catch (Exception e) {
			System.out.println(ERROR_ON_MAPPER2_2 + e.getLocalizedMessage());
		}
	}
}