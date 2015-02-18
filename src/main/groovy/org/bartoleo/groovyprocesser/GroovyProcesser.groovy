package org.bartoleo.groovyprocesser

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
    String lastInputText = ""
    String lastGroovyText = ""
    Processer processer = new Processer()

    public static void main(String[] args) {
        new GroovyProcesser().gui();
    }

    public GroovyProcesser() {

    }

    public void gui() {

        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "GroovyProcesser")

        int height = 800
        int width = 1024

        swing = new SwingBuilder()
        swing.edt {
            //lookAndFeel 'nimbus'
            frame(title: "Groovy Processer", pack: true, show: true,
                    defaultCloseOperation: EXIT_ON_CLOSE, id: "frame",
                    extendedState: JFrame.MAXIMIZED_BOTH,
                    preferredSize: [width, height],
            ) {
                splitPane(id: 'vsplit1', orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: (height/3*2) as int) {
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
                        splitPane(id: 'vsplit2', orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: (height/3) as int, constraints: BorderLayout.CENTER) {
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
                                    comboBox( id:'cmbFile')
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
            cmbFile.addItem("")
            cmbFile.addItem("test")
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
                try {
                    lastInputText = editorInput.text
                    lastGroovyText = editorGroovy.text

                    editorOutput.text = processer.process(editorInput.text, editorInput, editorGroovy.text)

                    editorOutput.foreground = java.awt.Color.BLACK
                } catch (Exception ex) {
                    editorOutput.text = ex
                    editorOutput.foreground = java.awt.Color.RED
                }
            }
        }
    }


}