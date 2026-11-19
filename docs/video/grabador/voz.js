// Narración por escena: la clave es el título del subtítulo o de la portada.
// Se lee con edge-tts (voz es-CO-SalomeNeural por defecto). Frases cortas y en lenguaje simple.
module.exports = {
  // 1 · Camino feliz
  'CrediFlow': 'Bienvenido a CrediFlow, una plataforma de originación de créditos. Vamos a recorrer el camino feliz: una persona pide un crédito y, en segundos, recibe la decisión, la cuota, su plan de pagos y una explicación clara.',
  'Panel de originación': 'Este es el panel. Muestra el total de solicitudes, la tasa de aprobación, el monto aprobado y el score promedio. Se actualiza solo cada cinco segundos.',
  'El recorrido de cada solicitud': 'Cada solicitud sigue este recorrido: entra por la API REST, se guarda junto con su evento en una sola transacción de Oracle, viaja por Amazon SNS y SQS, y la evalúa un worker con PL/SQL, S3, inteligencia artificial y un servicio SOAP.',
  'Actividad de la semana': 'Debajo, la actividad de los últimos siete días, el desglose por producto y las solicitudes más recientes.',
  'Simulador de cuota': 'Antes de pedir el crédito, el simulador calcula la cuota. Lo hace Oracle, con el mismo paquete PL/SQL que luego evalúa la solicitud, así que el simulador y la decisión nunca se contradicen.',
  'Cambiar de producto': 'Si cambio de producto, la tasa y la cuota se recalculan al instante.',
  'Solicitar el crédito': 'La cuota me convence, así que solicito el crédito desde aquí mismo.',
  'Paso 1 · Tus datos': 'El asistente tiene tres pasos. Primero, los datos de la persona: nombre, documento y correo. Cada campo se valida mientras se escribe.',
  'Paso 2 · Tus finanzas': 'Segundo, sus finanzas: ingresos y gastos mensuales. La capacidad de pago y el nivel de endeudamiento se calculan en vivo.',
  'Paso 3 · Tu crédito': 'Tercero, el crédito. Elijo vehículo. La cuota estimada y el anillo muestran qué parte de la capacidad de pago ocuparía. El máximo permitido es el cuarenta por ciento.',
  'Enviar la solicitud': 'Envío. La API valida los datos, guarda la solicitud y su evento en una sola transacción, y responde al instante, sin esperar la evaluación. Además, la solicitud lleva una clave de idempotencia: un doble clic nunca la duplica.',
  'Solicitud recibida': 'Solicitud recibida. Vamos a seguir su evaluación en vivo.',
  'Evaluación en vivo': 'La pantalla se actualiza sola. El publicador envía el evento a SNS, la cola SQS lo entrega al worker, que archiva el original en S3, calcula el score en PL/SQL, genera la explicación y registra el crédito en el core bancario por SOAP.',
  'Resultado de la evaluación': 'Aprobada. El paquete PKG CRÉDITO calculó el score, el nivel de riesgo, la tasa según ese riesgo y la cuota con el sistema francés. Debajo está la referencia que devolvió el core bancario.',
  'Explicación en lenguaje claro': 'Y aquí, la explicación de la decisión en palabras simples. La genera Claude, el modelo de Anthropic, sin enviarle datos personales. Si la inteligencia artificial no está disponible, el sistema usa una explicación por reglas, como en este caso, y la etiqueta lo indica.',
  'Historial de estados': 'El historial muestra cada cambio de estado con su hora exacta. Lo registra un trigger en Oracle, no la aplicación, así que nada queda sin auditar.',
  'Intentos del worker': 'Y aquí, cada intento del worker con su duración. Esta solicitud se evaluó en el primer intento.',
  'Tabla de amortización': 'Como fue aprobada, tiene su tabla de amortización: cuota fija, capital, interés y saldo mes a mes. La genera PL/SQL con una inserción masiva.',
  'Original en Amazon S3': 'El original de la solicitud quedó archivado en Amazon S3, cifrado y organizado por año, mes y producto, listo para auditoría y analítica.',
  'Buscar solicitudes': 'En solicitudes busco por nombre, documento o clave. También se puede filtrar por estado y producto, ordenar y paginar.',
  'Encontrada': 'Ahí está la solicitud, aprobada, con su score.',
  'De vuelta al panel': 'De vuelta al panel, los indicadores ya incluyen la solicitud nueva.',
  'Decidido en segundos': 'Eso es CrediFlow: Java y Spring Boot, reglas de negocio en PL/SQL, Angular, servicios de AWS creados con Terraform, integración REST y SOAP, e inteligencia artificial que explica cada decisión. Gracias por ver.',
};
