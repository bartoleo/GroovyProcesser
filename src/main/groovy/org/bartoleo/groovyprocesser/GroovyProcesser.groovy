package org.bartoleo.groovyprocesser

import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder

import javax.swing.*
import java.awt.*

import static javax.swing.JFrame.EXIT_ON_CLOSE

class GroovyProcesser {
    def swing;
    Font font = new Font("Courier", Font.PLAIN, 13)
    final static Binding binding = new Binding()
    final static GroovyShell shell = new GroovyShell(binding)
    String lastInputText = ""
    String lastGroovyText = ""

    public static void main(String[] args) {
        new GroovyProcesser().run();
    }

    public GroovyProcesser() {

    }

    public void run() {

        swing = new SwingBuilder()
        swing.edt {
            //lookAndFeel 'nimbus'
            frame(title: "Groovy Processer", pack: true, show: true,
                    defaultCloseOperation: EXIT_ON_CLOSE, id: "frame",
                    extendedState: JFrame.MAXIMIZED_BOTH,
                    preferredSize: [1024, 800],
            ) {
                splitPane(id: 'vsplit1', orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: 480) {
                    splitPane(id: 'vsplit2', orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: 320) {
                        scrollPane() {
                            editorPane(id: "editorInput", editable: true, font: font,
                                    keyReleased: { evt ->
                                        evaluate();
                                    }
                            )
                        }
                        scrollPane() {
                            editorPane(id: "editorGroovy", editable: true, font: font,
                                    text: '''input.eachLine{
  println "prefisso${it}suffisso"
}''',
                                    keyReleased: { evt ->
                                        evaluate();
                                    }
                            )
                        }
                    }
                    scrollPane() {
                        editorPane(id: "editorOutput", editable: false, font: font, background: new java.awt.Color (255, 255, 220))
                    }
                }
            }
        }
        swing.doLater {
            //frame.size = [1024,800]
        }
    }


    public void evaluate() {
        swing.doLater {
            if (editorInput.text != lastInputText || editorGroovy.text != lastGroovyText) {
                def saveOut = System.out
                try {
                    lastInputText = editorInput.text
                    lastGroovyText = editorGroovy.text

                    def buf = new ByteArrayOutputStream()
                    def newOut = new PrintStream(buf)
                    System.out = newOut

                    binding.setVariable("input", evaluateInput(editorInput.text))
                    binding.setVariable("setInput", { valore -> binding.setVariable("input", valore); editorInput.text = valore; })
                    def result = shell.evaluate(editorGroovy.text)

                    if (result) {
                        editorOutput.text = result.toString()
                    } else {
                        editorOutput.text = ""
                    }

                    System.out = saveOut

                    editorOutput.text = editorOutput.text + buf.toString()
                    editorOutput.foreground = java.awt.Color.BLACK
                } catch (Exception ex) {
                    editorOutput.text = ex
                    editorOutput.foreground = java.awt.Color.RED
                    System.out = saveOut
                }
            }
        }
    }

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

}