package org.bartoleo.groovyprocesser

class GroovyProcesser {

    public static void main(String[] args) {
        //no args then gui
        if (!args){
            new GroovyProcesserGui().showGui()
            return
        }

        //args then cli
        new GroovyProcesserCli(args).run()

    }

}