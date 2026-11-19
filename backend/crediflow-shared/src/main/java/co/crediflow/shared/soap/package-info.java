/**
 * Contrato SOAP del core bancario (ver core-bancario.xsd en crediflow-core-bancario).
 * Lo comparten el servidor (mock del core) y el cliente (worker).
 */
@XmlSchema(namespace = CoreBancarioNs.URI, elementFormDefault = XmlNsForm.QUALIFIED)
package co.crediflow.shared.soap;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
