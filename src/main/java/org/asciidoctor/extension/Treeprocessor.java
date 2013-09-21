package org.asciidoctor.extension;

import org.asciidoctor.internal.DocumentRuby;

public abstract class Treeprocessor extends Processor {

    public Treeprocessor(DocumentRuby documentRuby) {
        super(documentRuby);
    }

    public abstract void process();
    
}
