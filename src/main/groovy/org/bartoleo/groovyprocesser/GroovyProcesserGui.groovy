package org.bartoleo.groovyprocesser

import groovy.swing.SwingBuilder
import groovy.ui.text.TextUndoManager

import javax.swing.*
import javax.swing.filechooser.FileFilter
import java.awt.*
import java.awt.datatransfer.*
import java.awt.event.KeyEvent
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static javax.swing.JFrame.EXIT_ON_CLOSE

class GroovyProcesserGui implements ProcesserOutputInterface {
    def swing;
    Font font
    String lastInputText
    String lastGroovyText
    String baseDir
    Processer processer
    final ThreadPoolExecutor executor
    AtomicInteger executionProgr

    public GroovyProcesserGui() {
        font = new Font("Courier", Font.PLAIN, 13)
        swing = new SwingBuilder()
        lastInputText = ""
        lastGroovyText = ""
        baseDir = System.getProperty("user.home") + File.separator + "GroovyProcesser"
        processer = new Processer(baseDir)
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>())
        executionProgr = new AtomicInteger(0)
    }

    public void showGui() {

        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "GroovyProcesser")

        int height = 800
        int width = 1024

        def editorGroovyUndoManager = new TextUndoManager()
        def editorInputUndoManager = new TextUndoManager()
        def menuModifier = KeyEvent.getKeyModifiersText(
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()).toLowerCase() + ' '

        swing.edt {
            //lookAndFeel 'nimbus'
            frame(title: "Groovy Processer", pack: true, show: true,
                    defaultCloseOperation: EXIT_ON_CLOSE, id: "frame",
                    extendedState: JFrame.MAXIMIZED_BOTH,
                    preferredSize: [width, height]) {

                actionUndo = swing.action(name: 'Undo', accelerator: menuModifier + 'Z') {
                    def undoManager
                    def component = swing.frame.mostRecentFocusOwner
                    if (component == swing.editorGroovy) {
                        undoManager = editorGroovyUndoManager
                    }
                    if (component == swing.editorInput) {
                        undoManager = editorInputUndoManager
                    }
                    if (undoManager && undoManager.canUndo()) {
                        undoManager.undo()
                        evaluateRealTime()
                    }

                }

                actionRedo = swing.action(name: 'Redo', accelerator: menuModifier + 'Y') {
                    def undoManager
                    def component = swing.frame.mostRecentFocusOwner
                    if (component == swing.editorGroovy) {
                        undoManager = editorGroovyUndoManager
                    }
                    if (component == swing.editorInput) {
                        undoManager = editorInputUndoManager
                    }
                    if (undoManager && undoManager.canRedo()) {
                        undoManager.redo()
                        evaluateRealTime()
                    }
                }

                actionRun = swing.action(name: 'Run', accelerator: shortcut('ENTER')) {
                    evaluate()
                }

                // menu
                menuBar {
                    menu('File', mnemonic: 'F') {
//                        menuItem(action: actionNew)
//                        menuItem(action: actionOpen)
//                        menuItem(action: actionSave)
//                        menuItem(action: actionSaveAs)
//                        separator()
//                        menuItem(action: actionExit)
                    }
                    menu('Edit', mnemonic: 'E') {
                        menuItem(action: actionUndo, mnemonic: 'Z')
                        menuItem(action: actionRedo, mnemonic: 'Y')
                        menuItem(action: actionRun)
//                        separator()
//                        menuItem(action: actionCut)
//                        menuItem(action: actionCopy)
//                        menuItem(action: actionPaste)
//                        separator()
//                        menuItem(action: actionClearOutput)
                    }
                    menu('Options', mnemonic: 'O') {
//                        menuItem(action: actionEditConnections)
//                        checkBoxMenuItem(id:'menuStopOnError', action: actionOptionsStopOnError)
//                        menuItem(action: actionOptionsSetTitle)
//                        //menuItem(id:'menuStopOnError', action: actionOptionsStopOnError)
//               menuItem 'Preferences'
                    }
                    menu('View', mnemonic: 'V') {
//                        menuItem(action: actionPrevTab)
//                        menuItem(action: actionNextTab)
//                        separator()
//                        menuItem(action: actionKeepTab)
//                        menuItem(action: actionRemoveTab)
                    }
                    menu('Help', mnemonic: 'H') {
//                        menuItem(action: actionAbout)
                    }
                }

                splitPane(id: 'vsplit1', orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: (height / 3 * 2) as int) {
                    panel {
                        borderLayout(vgap: 5)
                        toolBar(rollover: true, constraints: BorderLayout.NORTH) {
                            button(text: "load", actionPerformed: {
                                loadInputAction()
                            })
                            button(text: "paste", actionPerformed: {
                                pasteInputAction()
                            })
                        }
                        splitPane(id: 'vsplit2', orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: (height / 3) as int, constraints: BorderLayout.CENTER) {
                            scrollPane() {
                                editorPane(id: "editorInput", editable: true, font: font,
                                        keyReleased: { evt ->
                                            if ((evt.isControlDown()) && (evt.getKeyCode() == 10 || evt.getKeyCode() == 13)) {
                                                evaluate()
                                            } else {
                                                evaluateRealTime()
                                            }
                                        }
                                )
                            }
                            panel {
                                borderLayout(vgap: 5)
                                toolBar(rollover: true, constraints: BorderLayout.NORTH) {
                                    button(text: "load", actionPerformed: {
                                        loadGroovyAction()
                                    })
                                    button(text: "save", actionPerformed: {
                                        saveGroovyAction()
                                    })
                                    separator()
                                    checkBox(id: 'chkRunEveryChange', text: 'Run Every Change', selected: true)
                                    separator()
                                    label 'Choose a file:'
                                    comboBox(id: 'cmbFile', actionPerformed: {
                                        selectedFile()
                                    })
                                }
                                scrollPane(id: "scrollPaneEditor") {
                                    editorPane(id: "editorGroovy", editable: true, font: font,
                                            keyReleased: { evt ->
                                                if (evt.isControlDown() && (evt.getKeyCode() == 10 || evt.getKeyCode() == 13)) {
                                                    evaluate()
                                                } else {
                                                    evaluateRealTime()
                                                }
                                            }
                                    )
                                }
                            }
                        }

                    }
                    panel {
                        borderLayout(vgap: 5)
                        toolBar(rollover: true, constraints: BorderLayout.NORTH) {
                            button(text: "copy", actionPerformed: {
                                copyAction()
                            })
                        }
                        scrollPane() {
                            editorPane(id: "editorOutput", editable: false, font: font, background: new java.awt.Color(255, 255, 220))
                        }
                    }
                }
            }
        }

        //add line number to groovy editor
        swing.scrollPaneEditor.rowHeaderView = new TextLineNumber(swing.editorGroovy)

        swing.editorGroovy.getDocument().addUndoableEditListener(editorGroovyUndoManager);
        swing.editorInput.getDocument().addUndoableEditListener(editorInputUndoManager);

        swing.doLater {
            //frame.size = [1024,800]
            loadCmbFile()
            editorGroovy.requestFocus()
            editorGroovy.text = '''\
input.eachLine{
  println "prefix ${it} suffix"
}'''
        }

    }

    private void selectedFile() {
        String selectedFile = swing.cmbFile.getSelectedItem()
        if (selectedFile) {
            swing.editorGroovy.text = new File(baseDir + File.separator + selectedFile).text
            evaluateRealTime()
        }
    }

    private void loadCmbFile() {
        DefaultComboBoxModel model = (DefaultComboBoxModel) swing.cmbFile.getModel();
        model.removeAllElements();
        swing.cmbFile.addItem("")
        File fileDir = new File(baseDir)
        def p = ~/.*\.groovy/
        fileDir.eachFileMatch(p) {
            swing.cmbFile.addItem(it.name)
        }
    }

    public void loadInputAction() {
        def openFileDialog = new JFileChooser(
                dialogTitle: "Choose an input file",
                fileSelectionMode: JFileChooser.FILES_ONLY)

        int userSelection = openFileDialog.showOpenDialog()
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = openFileDialog.getSelectedFile();
            swing.editorInput.text = fileToOpen.text
            evaluateRealTime()
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
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            }
            catch (UnsupportedFlavorException | IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
        return result;
    }

    public void pasteInputAction() {
        swing.doLater {
            editorInput.text = getClipboardContents();
            evaluateRealTime()
        }
    }


    public void loadGroovyAction() {
        def openFileDialog = new JFileChooser(
                dialogTitle: "Choose an input groovy file",
                currentDirectory: new File(baseDir),
                fileSelectionMode: JFileChooser.FILES_ONLY,
                fileFilter: [getDescription: {-> "*.groovy" }, accept: { file -> file ==~ /.*?\.groovy/ || file.isDirectory() }] as FileFilter)

        int userSelection = openFileDialog.showOpenDialog()
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = openFileDialog.getSelectedFile();
            swing.editorGroovy.text = fileToOpen.text
            evaluateRealTime()
        }
    }

    public void saveGroovyAction() {
        def saveFileDialog = new JFileChooser(
                dialogTitle: "Choose file to save",
                currentDirectory: new File(baseDir),
                fileSelectionMode: JFileChooser.FILES_ONLY,
                //the file filter must show also directories, in order to be able to look into them
                fileFilter: [getDescription: {-> "*.groovy" }, accept: { file -> file ==~ /.*?\.groovy/ || file.isDirectory() }] as FileFilter)

        int userSelection = saveFileDialog.showSaveDialog()
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = saveFileDialog.getSelectedFile();
            fileToSave.write(swing.editorGroovy.text)
        }
        loadCmbFile()
    }

    public void copyAction() {
        swing.doLater {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(editorOutput.text), null)
        }
    }

    public void evaluateRealTime() {
        swing.doLater {
            if (chkRunEveryChange.selected) {
                if (editorInput.text != lastInputText || editorGroovy.text != lastGroovyText) {
                    evaluate();
                }
            }
        }
    }

    public void evaluate() {
        lastInputText = swing.editorInput.text
        lastGroovyText = swing.editorGroovy.text
        //evaluate on other thread but sequentially and one at a time
//        println "queue:"+executor.getQueue().size()
        int lastProgr = executionProgr.incrementAndGet()
        executor.submit(
                {
                    try {
    //                    //there are next tasks
    //                    if (executor.getQueue().size() > 1) {
    //                        //autokill
    //                        return
    //                    }
                        //if i'm not the last... stop
                        if (executionProgr.intValue()!=lastProgr){
                            //autokill
                            return
                        }
                        // time for another keyboard click
                        sleep(50);
                        //if i'm not the last... stop
                        if (executionProgr.intValue()!=lastProgr){
                            //autokill
                            return
                        }
                        println "evaluate "+new Date()
                        processer.process(swing.editorInput.text, swing.editorGroovy.text, this)
                    } catch (Throwable ex){
                        println ex
                        ex.printStackTrace()
                    }
                } as Callable
        )
    }


    public void setInput(String pInputText) {
        swing.doLater {
            editorInput.text = pInputText
        }
    }

    public void setOutput(String pText) {
        swing.doLater {
            editorOutput.foreground = java.awt.Color.BLACK
            editorOutput.text = pText
        }
    }

    public void setOutputOnException(Throwable ex) {
        swing.doLater {
            editorOutput.foreground = java.awt.Color.RED
            StringWriter stackTraceWriter = new StringWriter()
            ex.printStackTrace(new PrintWriter(stackTraceWriter))
            editorOutput.text = ex.message + "\n\n" + stackTraceWriter.toString()
            editorOutput.setCaretPosition(0)
            editorOutput.moveCaretPosition(0)
        }
    }


}