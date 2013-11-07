package mx.gob.inr.seguimientoHosp.agendas

import mx.gob.inr.seguimientoHosp.Agenda;

class AgendaUltraSonido extends Agenda {
	   
   static mapping = {
	   table 'agendaultrasonido'
	   id column:'idcita'
	   paciente column:'idpaciente'
	   estudio column:'estudio'
	   version false
   }
}
