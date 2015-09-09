/**
 * PDFDomTree.java
 * (c) Radek Burget, 2011
 *
 * Pdf2Dom is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * Pdf2Dom is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License
 * along with CSSBox. If not, see <http://www.gnu.org/licenses/>.
 *
 * Created on 13.9.2011, 14:17:24 by burgetr
 */
package org.fit.pdfdom;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.pdfbox.exceptions.WrappedIOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 * A DOM representation of a PDF file.
 * 
 * @author burgetr
 */
public class PDFDomTree extends PDFBoxTree
{
    /** Default style placed in the begining of the resulting document */
    protected String defaultStyle = ".page{position:relative; border:1px solid blue;margin:0.5em}\n" +
    										   ".p,.r{position:absolute;}";
    
    /** The resulting document representing the PDF file. */
    protected Document doc;
    /** The head element of the resulting document. */
    protected Element head;
    /** The body element of the resulting document. */
    protected Element body;
    /** The title element of the resulting document. */
    protected Element title;
    /** The element representing the page currently being created in the resulting document. */
    protected Element curpage;
    
    /** Text element counter for assigning IDs to the text elements. */
    protected int textcnt;
    /** Page counter for assigning IDs to the pages. */
    protected int pagecnt;
    
    
    /**
     * Creates a new PDF DOM parser.
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public PDFDomTree() throws IOException, ParserConfigurationException
    {
        super();
        init();
    }
    
    /**
     * Internal initialization.
     * @throws ParserConfigurationException
     */
    private void init() throws ParserConfigurationException
    {
        pagecnt = 0;
        textcnt = 0;
    }
    
    /**
     * Creates a new empty HTML document tree.
     * @throws ParserConfigurationException
     */
    protected void createDocument() throws ParserConfigurationException
    {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        DocumentType doctype = builder.getDOMImplementation().createDocumentType("html", "-//W3C//DTD XHTML 1.1//EN", "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd");
        doc = builder.getDOMImplementation().createDocument("http://www.w3.org/1999/xhtml", "html", doctype);
        
        head = doc.createElement("head");
        Element meta = doc.createElement("meta");
        meta.setAttribute("http-equiv", "content-type");
        meta.setAttribute("content", "text/html;charset=utf-8");
        head.appendChild(meta);
        title = doc.createElement("title");
        title.setTextContent("PDF Document");
        head.appendChild(title);
        Element gs = doc.createElement("style");
        gs.setAttribute("type", "text/css");
        gs.setTextContent(defaultStyle);
        head.appendChild(gs);
        
        body = doc.createElement("body");
        
        Element root = doc.getDocumentElement();
        root.appendChild(head);
        root.appendChild(body);
    }
    
    /**
     * Obtains the resulting document tree.
     * @return The DOM root element.
     */
    public Document getDocument()
    {
        return doc;
    }
    
    @Override
    public void processDocument(PDDocument document, int startPage, int endPage)
            throws IOException
    {
    	try {
    		createDocument();
    	} catch (ParserConfigurationException e) {
            throw new WrappedIOException("Error: parser configuration error", e);
    	}
        super.processDocument(document, startPage, endPage);
        //use the PDF title
        String doctitle = document.getDocumentInformation().getTitle();
        if (doctitle != null && doctitle.trim().length() > 0)
            title.setTextContent(doctitle);
    }

    /**
     * Parses a PDF document and serializes the resulting DOM tree to an output. This requires
     * a DOM Level 3 capable implementation to be available.
     */
    @Override
    public void writeText(PDDocument doc, Writer outputStream) throws IOException
    {
        try
        {
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
            LSSerializer writer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            writer.getDomConfig().setParameter("format-pretty-print", true);
            output.setCharacterStream(outputStream);
            processDocument(doc);
            writer.write(getDocument(), output);
        } catch (ClassCastException e) {
            throw new WrappedIOException("Error: cannot initialize the DOM serializer", e);
        } catch (ClassNotFoundException e) {
            throw new WrappedIOException("Error: cannot initialize the DOM serializer", e);
        } catch (InstantiationException e) {
            throw new WrappedIOException("Error: cannot initialize the DOM serializer", e);
        } catch (IllegalAccessException e) {
            throw new WrappedIOException("Error: cannot initialize the DOM serializer", e);
        }
    }
    
    //===========================================================================================
    
    @Override
    protected void startNewPage()
    {
        curpage = createPageElement();
        body.appendChild(curpage);
    }
    
    @Override
    protected void renderText(String data)
    {
    	curpage.appendChild(createTextElement(data));
    }

    @Override
    protected void renderPath(List<PathSegment> path, boolean stroke, boolean fill)
    {
        float[] rect = toRectangle(path);
        if (rect != null)
        {
            System.err.println("YES a rectangle");
            curpage.appendChild(createRectangleElement(rect[0], rect[1], rect[2]-rect[0], rect[3]-rect[1], stroke, fill));
        }
        else
        {
            System.err.println("NOT a rectangle");
        }
    }
    
    /*protected void renderRectangle(RectPath rect, boolean stroke, boolean fill)
    {
    	curpage.appendChild(createRectangleElement(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), stroke, fill));
    }*/
    
    @Override
    protected void renderImage(float x, float y, float width, float height, String mimetype, byte[] data)
    {
    	curpage.appendChild(createImageElement(x, y, width, height, mimetype, data));
    }
    
    //===========================================================================================
    
    /**
     * Creates an element that represents a single page.
     * @return the resulting DOM element
     */
    protected Element createPageElement()
    {
        String pstyle = "";
        PDRectangle layout = getCurrentMediaBox();
        if (layout != null)
        {
            System.out.println("x1 " + layout.getLowerLeftX());
            System.out.println("y1 " + layout.getLowerLeftY());
            System.out.println("x2 " + layout.getUpperRightX());
            System.out.println("y2 " + layout.getUpperRightY());
            System.out.println("rot " + pdpage.findRotation());
            pstyle = "width:" + layout.getUpperRightY() + UNIT + ";"
                     + "height:" + layout.getUpperRightX() + UNIT;
        }
        else
            System.err.println("Warning: no media box found");
        
        Element el = doc.createElement("div");
        el.setAttribute("id", "page_" + (pagecnt++));
        el.setAttribute("class", "page");
        el.setAttribute("style", pstyle);
        return el;
    }
    
    /**
     * Creates an element that represents a single positioned box with no content.
     * @return the resulting DOM element
     */
    protected Element createTextElement()
    {
        Element el = doc.createElement("div");
        el.setAttribute("id", "p" + (textcnt++));
        el.setAttribute("class", "p");
        el.setAttribute("style", curstyle.toString());
        return el;
    }
    
    /**
     * Creates an element that represents a single positioned box containing the specified text string.
     * @param data the text string to be contained in the created box.
     * @return the resulting DOM element
     */
    protected Element createTextElement(String data)
    {
        Element el = createTextElement();
        Text text = doc.createTextNode(data);
        el.appendChild(text);
        return el;
    }

    /**
     * Creates an element that represents a rectangle drawn at the specified coordinates in the page.
     * @param x the X coordinate of the rectangle
     * @param y the Y coordinate of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param stroke should there be a stroke around?
     * @param fill should the rectangle be filled?
     * @return the resulting DOM element
     */
    protected Element createRectangleElement(float x, float y, float width, float height, boolean stroke, boolean fill)
    {
    	String color = "black";
    	if (strokingColor != null)
    		color = strokingColor;

    	StringBuilder pstyle = new StringBuilder(50);
    	pstyle.append("left:").append(style.formatLength(x)).append(';');
        pstyle.append("top:").append(style.formatLength(y)).append(';');
        pstyle.append("width:").append(style.formatLength(width)).append(';');
        pstyle.append("height:").append(style.formatLength(height)).append(';');
    	    
    	if (stroke)
    	{
        	//float old = lineWidth;
        	lineWidth = transformLength((float) getGraphicsState().getLineWidth());
        	/*if (lineWidth != old)
        		System.out.println("LW:" + old + "->" + lineWidth);*/
        	String lw = lineWidth == 0 ? "1px" : lineWidth + "pt";
        	pstyle.append("border:").append(lw).append(" solid ").append(color).append(';');
    	}
    	
    	if (fill)
    	{
    	    pstyle.append("background-color:").append(style.getColor()).append(';');
    	}
    	
        Element el = doc.createElement("div");
        el.setAttribute("class", "r");
        el.setAttribute("style", pstyle.toString());
        el.appendChild(doc.createEntityReference("nbsp"));
        return el;
    }

    /**
     * Creates an element that represents an image drawn at the specified coordinates in the page.
     * @param x the X coordinate of the image
     * @param y the Y coordinate of the image
     * @param width the width coordinate of the image
     * @param height the height coordinate of the image
     * @param type the image type: <code>"png"</code> or <code>"jpeg"</code>
     * @param data the image data depending on the specified type
     * @return
     */
    protected Element createImageElement(float x, float y, float width, float height, String mimetype, byte[] data)
    {
        StringBuilder pstyle = new StringBuilder("position:absolute;");
        pstyle.append("left:").append(x).append(UNIT).append(';');
        pstyle.append("top:").append(y).append(UNIT).append(';');
        pstyle.append("width:").append(width).append(UNIT).append(';');
        pstyle.append("height:").append(height).append(UNIT).append(';');
        //pstyle.append("border:1px solid red;");
        
        Element el = doc.createElement("img");
        el.setAttribute("style", pstyle.toString());
        
        if (!disableImageData)
        {
            char[] cdata = Base64Coder.encode(data);
            String imgdata = "data:" + mimetype + ";base64," + new String(cdata);
            el.setAttribute("src", imgdata);
        }
        else
            el.setAttribute("src", "");
        
        return el;
    }
    
}
