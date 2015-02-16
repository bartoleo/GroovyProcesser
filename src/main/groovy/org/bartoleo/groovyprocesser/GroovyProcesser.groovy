package org.bartoleo.groovyprocesser

import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder

import javax.swing.*
import java.awt.*
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.DataFlavor
import javax.swing.filechooser.FileFilter
import javax.swing.JFileChooser
import java.awt.datatransfer.UnsupportedFlavorException

import static javax.swing.JFrame.EXIT_ON_CLOSE

class GroovyProcesser {
    def swing;
    Font font = new Font("Courier", Font.PLAIN, 13)
    final static Binding binding = new Binding()
    final static GroovyShell shell = new GroovyShell(binding)
    String lastInputText = ""
    String lastGroovyText = ""

    public static void main(String[] args) {
        new GroovyProcesser().gui();
    }

    public GroovyProcesser() {

    }

    public void gui() {

        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "GroovyProcesser")

        swing = new SwingBuilder()
        swing.edt {
            //lookAndFeel 'nimbus'
            frame(title: "Groovy Processer", pack: true, show: true,
                    defaultCloseOperation: EXIT_ON_CLOSE, id: "frame",
                    extendedState: JFrame.MAXIMIZED_BOTH,
                    preferredSize: [1024, 800],
            ) {
                splitPane(id: 'vsplit1', orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: 480) {
                    panel {
                        borderLayout(vgap: 5)
                        toolBar(rollover: true, constraints: BorderLayout.NORTH){
                            button( text:"load", actionPerformed:{
                                loadInputAction()
                            })
                            button( text:"paste", actionPerformed:{
                                pasteInputAction()
                            })
                        }
                        splitPane(id: 'vsplit2', orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: 320, constraints: BorderLayout.CENTER) {
                            scrollPane() {
                                editorPane(id: "editorInput", editable: true, font: font,
                                        keyReleased: { evt ->
                                            evaluate();
                                        }
                                )
                            }
                            panel {
                                borderLayout(vgap: 5)
                                toolBar(rollover: true, constraints: BorderLayout.NORTH){
                                    button( text:"load", actionPerformed:{
                                        loadGroovyAction()
                                    })
                                    button( text:"save", actionPerformed:{
                                        saveGroovyAction()
                                    })
                                }
                                scrollPane(id: "scrollPaneEditor") {
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
                        }

                    }
                    panel {
                        borderLayout(vgap: 5)
                        toolBar(rollover: true, constraints: BorderLayout.NORTH){
                            button( text:"copy", actionPerformed:{
                                copyAction()
                            })
                        }
                        scrollPane() {
                        editorPane(id: "editorOutput", editable: false, font: font, background: new java.awt.Color (255, 255, 220))
                        }
                    }
                }
            }
        }
        swing.scrollPaneEditor.rowHeaderView = new TextLineNumber(swing.editorGroovy)
        swing.doLater {
            //frame.size = [1024,800]
        }
    }

    public void loadInputAction(){
        def openFileDialog = new JFileChooser(
                dialogTitle: "Choose an input file",
                fileSelectionMode: JFileChooser.FILES_ONLY)

        int userSelection = openFileDialog.showOpenDialog()
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = openFileDialog.getSelectedFile();
            swing.editorInput.text = fileToOpen.text
            evaluate()
        }
    }

    public String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //odd: the Object param of getContents is not currently used
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText =
            (contents != null) &&
                    contents.isDataFlavorSupported(DataFlavor.stringFlavor)
        ;
        if (hasTransferableText) {
            try {
                result = (String)contents.getTransferData(DataFlavor.stringFlavor);
            }
            catch (UnsupportedFlavorException | IOException ex){
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
        return result;
    }

    public void pasteInputAction(){
        swing.doLater {
            editorInput.text = getClipboardContents();
            evaluate()
        }
    }


    public void loadGroovyAction(){
        def openFileDialog = new JFileChooser(
                dialogTitle: "Choose an input groovy file",
                fileSelectionMode: JFileChooser.FILES_ONLY,
                fileFilter: [getDescription: {-> "*.groovy"}, accept:{file-> file ==~ /.*?\.groovy/ || file.isDirectory() }] as FileFilter)

        int userSelection = openFileDialog.showOpenDialog()
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = openFileDialog.getSelectedFile();
            swing.editorGroovy.text = fileToOpen.text
            evaluate()
        }
    }

    public void saveGroovyAction(){
        def saveFileDialog = new JFileChooser(
                dialogTitle: "Choose file to save",
                fileSelectionMode: JFileChooser.FILES_ONLY,
                //the file filter must show also directories, in order to be able to look into them
                fileFilter: [getDescription: {-> "*.groovy"}, accept:{file-> file ==~ /.*?\.groovy/ || file.isDirectory() }] as FileFilter)

        int userSelection = saveFileDialog.showSaveDialog()
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = saveFileDialog.getSelectedFile();
            fileToSave.write(swing.editorGroovy.text)
        }
    }

    public void copyAction(){
        swing.doLater {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(editorOutput.text), null)
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