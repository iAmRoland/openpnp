//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.10.23 at 08:50:01 AM PDT
//


package org.openpnp.model.eagle.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"value"})
@XmlRootElement(name = "description")
public class Description {

    @XmlAttribute(name = "language")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String language;
    @XmlValue
    protected String value;

    /**
     * Gets the value of the language property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getLanguage() {
        if (language == null) {
            return "en";
        }
        else {
            return language;
        }
    }

    /**
     * Sets the value of the language property.
     * 
     * @param value allowed object is {@link String }
     * 
     */
    public void setLanguage(String value) {
        this.language = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getvalue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value allowed object is {@link String }
     * 
     */
    public void setvalue(String value) {
        this.value = value;
    }

}