package co.crediflow.core;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/** Se traduce a un SOAP Fault de tipo Server: el cliente sabe que puede reintentar. */
@SoapFault(faultCode = FaultCode.SERVER)
public class CoreNoDisponibleException extends RuntimeException {

    public CoreNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
