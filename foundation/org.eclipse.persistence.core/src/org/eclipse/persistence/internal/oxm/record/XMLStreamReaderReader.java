/*******************************************************************************
* Copyright (c) 1998, 2011 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
* which accompanies this distribution.
* The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
* and the Eclipse Distribution License is available at
* http://www.eclipse.org/org/documents/edl-v10.php.
*
* Contributors:
*     bdoughan - June 24/2009 - 2.0 - Initial implementation
******************************************************************************/
package org.eclipse.persistence.internal.oxm.record;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamReader;

import org.eclipse.persistence.internal.oxm.record.namespaces.UnmarshalNamespaceContext;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLUnmarshaller;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Convert an XMLStreamReader into SAX events.
 */
public class XMLStreamReaderReader extends XMLReaderAdapter {

    private int depth = 0;
    private UnmarshalNamespaceContext unmarshalNamespaceContext;
    private XMLStreamReaderAttributes indexedAttributeList;
    private boolean qNameAware;

    public XMLStreamReaderReader() {
        unmarshalNamespaceContext = new UnmarshalNamespaceContext();
        indexedAttributeList = new XMLStreamReaderAttributes();
    }

    public XMLStreamReaderReader(XMLUnmarshaller xmlUnmarshaller) {
        super(xmlUnmarshaller);
        unmarshalNamespaceContext = new UnmarshalNamespaceContext();
        indexedAttributeList = new XMLStreamReaderAttributes();
    }

    @Override
    public void setContentHandler (ContentHandler handler) {
        super.setContentHandler(handler);
        Class handlerClass = handler.getClass();
        if(handlerClass == UnmarshalRecord.class){
            ((UnmarshalRecord)handler).setUnmarshalNamespaceResolver(unmarshalNamespaceContext);
            qNameAware = false;
        }else if(handlerClass == SAXUnmarshallerHandler.class){
            ((SAXUnmarshallerHandler)handler).setUnmarshalNamespaceResolver(unmarshalNamespaceContext);
            qNameAware = true;
        } else {
            qNameAware = true;
        }
    }

    @Override
    public void parse(InputSource input) throws SAXException {
        if(null == contentHandler) {
            return;
        }
        if(input instanceof XMLStreamReaderInputSource) {
            XMLStreamReader xmlStreamReader = ((XMLStreamReaderInputSource) input).getXmlStreamReader();
            unmarshalNamespaceContext.setXmlStreamReader(xmlStreamReader);
            indexedAttributeList.setXmlStreamReader(xmlStreamReader);
            parse(xmlStreamReader);
        }
    }

    private void parse(XMLStreamReader xmlStreamReader) throws SAXException {
        try {
            contentHandler.startDocument();
            parseEvent(xmlStreamReader);
            while(depth > 0) {
                xmlStreamReader.next();
                parseEvent(xmlStreamReader);
            }
            contentHandler.endDocument();
        } catch(SAXException e ) {
            throw e;
        } catch(Exception e) {
            throw new SAXException(e);
        }
    }

    private void parseEvent(XMLStreamReader xmlStreamReader) throws SAXException {
        switch (xmlStreamReader.getEventType()) {
            case XMLStreamReader.START_ELEMENT: {
                depth++;
                String localName = xmlStreamReader.getLocalName();
                String namespaceURI = xmlStreamReader.getNamespaceURI();
                if(XMLConstants.EMPTY_STRING.equals(namespaceURI)) {
                    namespaceURI = null;
                }
                if(qNameAware) {
                    String prefix = xmlStreamReader.getPrefix();
                    if(null == prefix || prefix.length() == 0) {
                        contentHandler.startElement(namespaceURI, localName, localName, indexedAttributeList.reset());
                    } else {
                        contentHandler.startElement(namespaceURI, localName, prefix + XMLConstants.COLON + localName, indexedAttributeList.reset());
                    }
                } else {
                    contentHandler.startElement(namespaceURI, localName, null, indexedAttributeList.reset());
                }
                break;
            }
            case XMLStreamReader.END_ELEMENT: {
                depth--;
                String localName = xmlStreamReader.getLocalName();
                String namespaceURI = xmlStreamReader.getNamespaceURI();
                if(XMLConstants.EMPTY_STRING.equals(namespaceURI)) {
                    namespaceURI = null;
                }
                if(qNameAware) {
                    String prefix = xmlStreamReader.getPrefix();
                    if(null == prefix || prefix.length() == 0) {
                        contentHandler.endElement(namespaceURI, localName, localName);
                    } else {
                        contentHandler.endElement(namespaceURI, localName, prefix + XMLConstants.COLON + localName);
                    }
                } else {
                    contentHandler.endElement(namespaceURI, localName, null);
                }
                break;
            }
            case XMLStreamReader.PROCESSING_INSTRUCTION: {
                contentHandler.processingInstruction(xmlStreamReader.getPITarget(), xmlStreamReader.getPIData());
                break;
            }
            case XMLStreamReader.CHARACTERS: {
                parseCharactersEvent(xmlStreamReader);
                break;
            }
            case XMLStreamReader.COMMENT: {
                if(null != lexicalHandler) {
                    lexicalHandler.comment(xmlStreamReader.getTextCharacters(), xmlStreamReader.getTextStart(), xmlStreamReader.getTextLength());
                }
                break;
            }
            case XMLStreamReader.SPACE: {
                contentHandler.characters(xmlStreamReader.getTextCharacters(), xmlStreamReader.getTextStart(), xmlStreamReader.getTextLength());
                break;
            }
            case XMLStreamReader.START_DOCUMENT: {
                depth++;
                break;
            }
            case XMLStreamReader.END_DOCUMENT: {
                depth--;
                return;
            }
            case XMLStreamReader.ENTITY_REFERENCE: {
                break;
            }
            case XMLStreamReader.ATTRIBUTE: {
                break;
            }
            case XMLStreamReader.DTD: {
                break;
            }
            case XMLStreamReader.CDATA: {
                char[] characters = xmlStreamReader.getText().toCharArray();
                if(null == lexicalHandler) {
                    parseCharactersEvent(xmlStreamReader);
                } else {
                    lexicalHandler.startCDATA();
                    parseCharactersEvent(xmlStreamReader);
                    lexicalHandler.endCDATA();
                }
                break;
            }
        }
    }

    /**
     * Subclasses of this class can override this method to provide alternate
     * mechanisms for processing the characters event.  One possibility is 
     * obtaining a CharSequence and calling the corresponding characters method 
     * on the extended content handler.
     */
    protected void parseCharactersEvent(XMLStreamReader xmlStreamReader) throws SAXException {
        contentHandler.characters(xmlStreamReader.getTextCharacters(), xmlStreamReader.getTextStart(), xmlStreamReader.getTextLength());
    }

    private static class XMLStreamReaderAttributes  extends IndexedAttributeList {

        private XMLStreamReader xmlStreamReader;

        public void setXmlStreamReader(XMLStreamReader xmlStreamReader) {
            this.xmlStreamReader = xmlStreamReader;
        }

        @Override
        protected List<Attribute> attributes() {
            if(null == attributes) {
                int namespaceCount = xmlStreamReader.getNamespaceCount();
                int attributeCount = xmlStreamReader.getAttributeCount();

                attributes = new ArrayList<Attribute>(attributeCount + namespaceCount);

                for(int x=0; x<namespaceCount; x++) {
                    String uri = XMLConstants.XMLNS_URL;
                    String localName = xmlStreamReader.getNamespacePrefix(x);
                    String qName;
                    if(null == localName || localName.length() == 0) {
                        localName = XMLConstants.XMLNS;
                        qName = XMLConstants.XMLNS;
                    } else {
                        qName = XMLConstants.XMLNS + XMLConstants.COLON + localName;
                    }
                    String value = xmlStreamReader.getNamespaceURI(x);
                    attributes.add(new Attribute(uri, localName, qName, value));
                }

                for(int x=0; x<attributeCount; x++) {
                    String uri = xmlStreamReader.getAttributeNamespace(x);
                    String localName = xmlStreamReader.getAttributeLocalName(x);
                    String prefix = xmlStreamReader.getAttributePrefix(x);
                    String qName;
                    if(null == prefix || prefix.length() == 0) {
                        qName = localName;
                    } else {
                        qName = prefix + XMLConstants.COLON + localName;
                    }
                    String value = xmlStreamReader.getAttributeValue(x);
                    attributes.add(new Attribute(uri, localName, qName, value));
                }
            }
            return attributes;
        }

    }

}