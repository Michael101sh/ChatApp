/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */
   
package classifier;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

/*Format the train data to lines of: word & the number of it occurrences*/
public class Reducer1 extends MapReduceBase implements
		Reducer<Text, IntWritable, Text, IntWritable> {

	private static final String ERROR_ON_REDUCER1 = "error on reducer1:\n";
	
	/*
	 * input: [word][<1 or -1>]
	 * output: [word][number of it occurrences]
	 */
	public void reduce(Text key, Iterator<IntWritable> values,
			OutputCollector<Text, IntWritable> output, Reporter reporter)
			throws IOException {

		try {
			int sum = 0;
			while (values.hasNext()) {
				sum += values.next().get();
			}
			output.collect(key, new IntWritable(sum));
		} catch (Exception e) {
			System.out.println(ERROR_ON_REDUCER1 + e.getLocalizedMessage());
		}
	}
}
