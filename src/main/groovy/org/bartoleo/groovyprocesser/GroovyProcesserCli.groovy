package org.bartoleo.groovyprocesser

class GroovyProcesserCli implements ProcesserOutputInterface {
    Processer processer
    String baseDir
    String groovyCode
    boolean interactive

    public GroovyProcesserCli(String[] args) {
        baseDir = System.getProperty("user.home") + File.separator + "GroovyProcesser"
        processer = new Processer(baseDir)
        groovyCode = new File(args[0]).text
        args.each {
            if (it == '-i') {
                interactive = true
            }
        }
    }

    public void run() {

        if (!interactive) {
            evaluate(System.in.text)
        } else {
            def inputstream = new InputStreamReader(System.in)
            def r = new BufferedReader(inputstream)

            System.out.println("Type input (empty to quit)!")
            String inputLine
            while (true) {
                inputLine = r.readLine()
                if (!inputLine) {
                    break
                }
                evaluate(inputLine)
            }
        }
    }


    public void evaluate(String pInput) {
        try {
            processer.process(pInput, this.groovyCode, this)
        } catch (Throwable ex) {
            println ex
            ex.printStackTrace()
        }
    }


    public void setInput(String pInputText) {

    }

    public void setOutput(String pText) {
        println pText
    }

    public void setOutputOnException(Throwable ex) {
        StringWriter stackTraceWriter = new StringWriter()
        ex.printStackTrace(new PrintWriter(stackTraceWriter))
        println ex.message + "\n\n" + stackTraceWriter.toString()
    }

}