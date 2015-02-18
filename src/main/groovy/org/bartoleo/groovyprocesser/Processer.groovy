package org.bartoleo.groovyprocesser

@Grab(group='org.apache.commons', module='commons-lang3', version='3.1')

import groovy.json.JsonSlurper
import org.apache.commons.lang3.text.WordUtils

import java.beans.Introspector

class Processer {

    final Binding binding = new Binding()
    final GroovyShell shell = new GroovyShell(binding)

    public def evaluateInput(String pText) {
        def result
        if (!pText) {
            return ""
        }
        result = pText
        if (pText.startsWith("/***")) {
            String firstLine = getFirstLine(pText)
            if (firstLine.endsWith("***/")) {
                String stringActions = firstLine.substring(4, firstLine.length() - 4);
                def actions = stringActions.split(",")
                result = stripFirstLine(pText)
                actions.each { action ->
                    if (action == "groovy") {
                        result = shell.evaluate(result)
                    }
                    if (action == "json") {
                        def slurper = new JsonSlurper()
                        result = slurper.parseText(result)
                    }
                    if (action == "xml") {
                        def slurper = new XmlSlurper()
                        result = slurper.parseText(result)
                    }
                    if (action == "url") {
                        //TODO: cache!!!!!!!
                        result = result.toURL().getText();
                    }
                    if (action == "file") {
                        result = new File(result).getText();
                    }
                    if (action == "cmd") {
                        def command = result
                        def proc = command.execute()
                        def outStream = new ByteArrayOutputStream(4096)
                        def errStream = new ByteArrayOutputStream(4096)
                        proc.waitForProcessOutput(outStream, errStream)
                        result = outStream.toString()
                    }

                }
            }
        }
        return result
    }

    public String getFirstLine(pText) {
        int index = pText.indexOf("\n")
        if (index >= 0) {
            return pText.substring(0, index)
        }
        return pText
    }

    public String stripFirstLine(pText) {
        int index = pText.indexOf("\n")
        if (index >= 0) {
            return pText.substring(index + 1)
        }
        return pText
    }

    def process(String pInput, final def pInputEditor, def pGroovyScript) {
        String result

        //redirect output to stream, so I'll read output written with print
        def saveOut = System.out
        def buf = new ByteArrayOutputStream()
        def newOut = new PrintStream(buf)
        System.out = newOut

        try {

            binding.setVariable("processer", this)
            binding.setVariable("input", evaluateInput(pInput))
            binding.setVariable("setInput", { valore -> binding.setVariable("input", valore); pInputEditor.text = valore; })
            def script = shell.parse(pGroovyScript)
            def returned = script.run()

            if (returned) {
                result = returned.toString()
            } else {
                result = ""
            }

            result +=  buf.toString()

        } finally {
            System.out = saveOut
        }

        return result

    }

    public String toPropertyName(String pLine){
        String propertyName

        if (!pLine){
            return ""
        }

        propertyName = WordUtils.capitalize(pLine)
        propertyName = propertyName.replace(" ","")

        return Introspector.decapitalize(propertyName)

    }

    public String toGetter(String pLine){
        String propertyName

        if (!pLine){
            return ""
        }

        propertyName = toPropertyName(pLine)

        return "get"+WordUtils.capitalize(propertyName)+"("+")"

    }

    public String toSetter(String pLine, String pValue){
        String propertyName

        if (!pLine){
            return ""
        }

        propertyName = toPropertyName(pLine)

        return "set"+WordUtils.capitalize(propertyName)+"("+pValue+")"

    }


}
