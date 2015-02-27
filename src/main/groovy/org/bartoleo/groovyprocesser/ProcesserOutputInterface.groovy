package org.bartoleo.groovyprocesser

interface ProcesserOutputInterface {
    public void setInput(String pInputText);
    public void setOutput(String pText);
    public void setOutputOnException(Throwable ex);
}
