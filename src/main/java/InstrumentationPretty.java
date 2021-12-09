import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.*;

/*
 * This Java source file was auto generated by running 'gradle buildInit --type java-library'
 * by 'james' at '12/16/18 7:41 PM' with Gradle 2.11
 *
 * @author james, @date 12/16/18 7:41 PM
 */
public class InstrumentationPretty {

    private int PIPE_TIMEOUT_SECONDS = 5*60;

    private String outputpath;

    private ExecutorService readerExecutor = Executors.newSingleThreadExecutor();
    public InstrumentationPretty(String outputpath){
        this.outputpath = outputpath;
    }

    /**
     * @return true if any tests failed
     */
    public boolean processInsturmentationOutput() throws IOException{
        //create test listener and parser
        XmlTestRunListener xmlTestListener = new XmlTestRunListener();
        HtmlTestRunListener htmlTestListener = new HtmlTestRunListener();
        InstrumentationResultParser parser = new InstrumentationResultParser("Instrumentation results", Arrays.asList(xmlTestListener, htmlTestListener));
        File reportDir = null;
        if(!this.outputpath.isEmpty()){
            reportDir = new File(outputpath);
        }else{
            //defaults to current directory/reports
            reportDir = new File(System.getProperty("user.dir") + "/reports");
        }
        reportDir.mkdir();
        xmlTestListener.setReportDir(reportDir);
        htmlTestListener.setReportDir(reportDir);
        List<String> lines = new ArrayList<String>();
        
        //read lines from STDIN 
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); 
        // Reading data using readLine
        String input = null;
        while( (input = readLineWithTimeout(reader, PIPE_TIMEOUT_SECONDS)) != null ){
            lines.add(input);

            if (!reader.ready()) {
                // Next read may block, flush the buffer
                parser.processNewLines(lines.toArray(new String[0]));
                lines.clear();
            }
        }

        // flush anything remaining in buffer
        parser.processNewLines(lines.toArray(new String[0]));
        parser.done();

        final TestRunResult overallResult = xmlTestListener.getRunResult();
        return overallResult.isRunFailure() || overallResult.hasFailedTests();
    }

    private String readLineWithTimeout(BufferedReader reader, int timeout) throws IOException {
        if (reader.ready()) {
            return reader.readLine();
        }

        Callable<String> readJob = reader::readLine;

        Future<String> future = readerExecutor.submit(readJob);
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        //Ignore timeout exception above so we still generate a report
        return null;
    }

    public static void main(String args[]){
        //process args
        Options options = new Options();

        Option output = new Option("o", "output", true, "output file path");
        output.setRequired(false);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("InstrumentationPretty", options);
            System.exit(1);
        }

        String outputFilePath = cmd.getOptionValue("output");
        try {
            final InstrumentationPretty resultParser;
            if(outputFilePath != null){
                resultParser = new InstrumentationPretty(outputFilePath);
            }else{
                resultParser = new InstrumentationPretty("");
            }

            boolean failures = resultParser.processInsturmentationOutput();
            System.exit(failures ? 2 : 0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
