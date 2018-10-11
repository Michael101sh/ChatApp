/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */
   
package classifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

/*Get identifier for the review (positive or negative)*/
public class Reducer2 extends MapReduceBase implements
		Reducer<Text, Text, Text, Text> {

	private static final int PARAM1 = 100;
	private static final int PARAM2 = 70;
	private static final String POSITIVE = "positive";
	private static final String NEGATIVE = "negative";
	private static final String PREFIX_A = "classifier";
	private static final String ERROR_ON_REDUCER2 = "error on reducer2:\n";

	/*
	 * input: ["key"]["classifier " + word + number of it occurrences] OR 
	 * 		["key"]["test " + word + number of it occurrences]
	 * output: "positive" or "negative"
	 */
	public void reduce(Text key, Iterator<Text> values,
			OutputCollector<Text, Text> output, Reporter reporter)
			throws IOException {

		try {
			Map<String, Integer> classify = new HashMap<String, Integer>();
			Map<String, Integer> test = new HashMap<String, Integer>();
			Integer totalReview = 0;

			while (values.hasNext()) {
				String line = values.next().toString();
				String[] splitted = line.split(" |	");

				if (splitted[0].equals(PREFIX_A)) {// from mapper2_1
					classify.put(splitted[1], Integer.parseInt(splitted[2]));
				} else {// from mapper2_2
					test.put(splitted[1], Integer.parseInt(splitted[2]));
				}
			}
			Iterator<String> itr = test.keySet().iterator();
			Integer revs = 0;
			while (itr.hasNext()) {
				String currentStr = itr.next();
				if (classify.containsKey(currentStr)) {
					revs = classify.get(currentStr)
							* Math.abs(test.get(currentStr));
					totalReview += (Math.abs(revs) > PARAM1 || revs > PARAM2) ? 0 : revs;
					System.out.println("str: " + currentStr + " | "
							+ totalReview);//////////////////////////////////
				}
			}
			String result = (totalReview < 0) ? NEGATIVE : POSITIVE;
			output.collect(new Text(result), new Text(""));
		} catch (Exception e) {
			System.out.println(ERROR_ON_REDUCER2 + e.getLocalizedMessage());
		}
	}
}