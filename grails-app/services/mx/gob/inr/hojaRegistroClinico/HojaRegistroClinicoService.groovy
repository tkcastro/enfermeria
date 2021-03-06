	package mx.gob.inr.hojaRegistroClinico

import mx.gob.inr.catalogos.CatProcedimientoNotaEnfermeria;
import mx.gob.inr.catalogos.CatRubroNotaEnfermeria;
import mx.gob.inr.utils.*;
import mx.gob.inr.seguridad.*;
import static mx.gob.inr.utils.ConstantesHojaEnfermeria.*
import grails.converters.*
import mx.gob.inr.reportes.Util


class HojaRegistroClinicoService {

	static transactional = true
	
	def signosVitalesService 
	def valoracionEnfermeriaService
	def utilService
	def controlLiquidosMedicamentosService
	def indicadoresCalidadService	
	
	/***
	 * 
	 * @param params
	 * @return la hoja
	 */
	HojaRegistroEnfermeria guardarHojaTurno(jsonHoja, Usuario usuario){

		def hoja = new HojaRegistroEnfermeria()

		if(jsonHoja.idHoja)
			hoja = HojaRegistroEnfermeria.get(jsonHoja.idHoja)

		
		hoja.otros = jsonHoja.otros
		hoja.alergias = jsonHoja.alergias
		hoja.peso = jsonHoja.peso as float
		hoja.talla = jsonHoja.talla as float
		
		hoja.fechaElaboracion = new Date().parse("dd/MM/yyyy",jsonHoja.fechaElaboracion)
		
		hoja.has = jsonHoja.has == 'on'
		hoja.dm = jsonHoja.dm == 'on'
		hoja.nef = jsonHoja.nef == 'on'
		hoja.ic = jsonHoja.ic == 'on'
		hoja.ir = jsonHoja.ir == 'on'

		hoja.asignarComorbilidad()

		hoja.admision = AdmisionHospitalaria.get(jsonHoja.idAdmision)
		hoja.paciente  = Paciente.get(jsonHoja.idPaciente)
		
		hoja.save([validate:false])
		
		
		if(!existeTurno(hoja.id, jsonHoja.turno)){		
		
			def hojaTurno = new HojaRegistroEnfermeriaTurno(
				hoja:hoja,
				usuario:usuario,
				turno:Turno."${jsonHoja.turno}",
				ipUsuario:utilService.getIpTerminal()							
			)
			
			hojaTurno.save([validate:false])
		}
		
		hoja

	}

	/***
	 * Consulta de manera completa hoja de enfermeria
	 * @param idHoja
	 * @return hoja
	 */
	def consultarHoja(Long idHoja, String turnoActual=null){

		def hoja = HojaRegistroEnfermeria.createCriteria().get{
			admision{
			}
			paciente{
			}
			eq("id",idHoja)
			maxResults(1)
		}
		
		if(hoja){		
			hoja.turnoActual = turnoActual
			
			hoja.signosVitales = signosVitalesService.consultarSignosVitales(idHoja)		
			hoja.dietas = signosVitalesService.consultarDietas(idHoja)
			hoja.requisitos = valoracionEnfermeriaService.consultarRequisitos(idHoja)
			
			
			hoja.rubrosValoracion = utilService.consultarCatalogoRubro(S_VALORACION)
			hoja.rubrosDiagnostico = utilService.consultarCatalogoRubro(S_DIAGNOSTICOS_INTERVENCIONES)
			hoja.rubrosIndicador = utilService.consultarCatalogoRubro(S_INDICADORES_CALIDAD,true)
			
			hoja.ingresos = controlLiquidosMedicamentosService.consultarIngresos(idHoja)
			hoja.medicamentos = controlLiquidosMedicamentosService.consultarMedicamentos(idHoja)
			hoja.escalaOtros = controlLiquidosMedicamentosService.consultarEscalaOtros(idHoja)
			
			
			hoja.escalaMadox = indicadoresCalidadService.consultarEscalaMadox(idHoja)
			hoja.indicadores = indicadoresCalidadService.consultarIndicadores(idHoja)
			hoja.diagEnfermeriaObservaciones = indicadoresCalidadService.consultarPlaneacionObservaciones(idHoja)
			
		}

		return hoja
	}
	
	/**
	 * Asigna la fecha de elaboracion de acuerdo a la hora
	 * @return
	 */
	Date asignarFechaElaboracion(){
		
		Date result = new Date()
		
		String horaActual = Util.getFechaActual("HH:mm");
		Integer hora = Integer.parseInt(horaActual.substring(0, 2));
		Integer minuto = Integer.parseInt(horaActual.substring(3, 5));
			
		if( hora >= 0 && hora <= 6)
			result = Util.getFechaAyer()
		
		if(hora == 7 && minuto <=29){
			result = Util.getFechaAyer()		
		}
		
		return result
		
	}
	
	
	/*****
	 * Hojas posteriores a la fecha actual modo edicion hasta las 9:59
	 * @param hoja
	 * @return
	 */
	boolean hojaSoloLectura(Date fechaElaboracion){
		
		boolean soloLectura = false
		
		Date fechaActual = Util.getFechaDate(Util.getFechaActual("yyyy-MM-dd"));
		Date fechaAyer = Util.getFechaAyer();
		
		String horaActual = Util.getFechaActual("HH:mm");
		Integer hora = Integer.parseInt(horaActual.substring(0, 2));
		Integer minuto = Integer.parseInt(horaActual.substring(3, 5));
		
		if(fechaElaboracion.compareTo(fechaActual) == 0){
			soloLectura = false
		}
		else{
			if(fechaElaboracion.compareTo(fechaAyer) == 0){
				if( hora <= 9 &&  minuto <= 59)
					soloLectura = false;
				else
					soloLectura = true;
			}
			else {
				soloLectura = true;
			}
		}	
		
		soloLectura
		
	}	
	
	
	def cargarHojaHistorica(Long idPaciente ,Date fechaElaboracion = null){
		
		def maxFechaElaboracion = HojaRegistroEnfermeria.createCriteria().get{
			projections{
				max("fechaElaboracion")
			}						
			eq("paciente.id",idPaciente)
			
			if(fechaElaboracion){
				ne("fechaElaboracion",fechaElaboracion)
			}
			
			maxResults(1)
		}
		
		HojaRegistroEnfermeria hoja = HojaRegistroEnfermeria.createCriteria().get{
			eq("paciente.id",idPaciente)
			eq("fechaElaboracion",maxFechaElaboracion)
			maxResults(1)
		}
		
		if(hoja){		
			hoja.dietas = signosVitalesService.consultarDietas(hoja.id)
			hoja.requisitos = valoracionEnfermeriaService.consultarRequisitos(hoja.id)
		}
		
		hoja			
		
	}
		
	def consultarHojas(Long idPaciente){
		
		def html = """

			<label>Asociar turno:</label>
			<select name="turnoAsociar" id="turnoAsociar">
				<option value="MATUTINO">MATUTINO</option>
				<option value="VESPERTINO">VESPERTINO</option>
				<option value="NOCTURNO">NOCTURNO</option>
			</select>	
			
			<div style="height:500px;overflow:auto;" class="wrapper" >
			<table id="tablaHojas">
			<thead>			
					<tr>						
						<th>
							Fecha<br>Elaboracion
						</th>
						<th>
							Matutino
						</th>
						<th>
							Vespertino
						</th>
						<th>
							Nocturno
						</th>
						<th>
							Cargar<br>
							Asociar
						</th>
						<th>
							Traslado<br>
							Paciente
						</th>
					</tr>			
			</thead><tbody>

		"""
		
		HojaRegistroEnfermeria.createCriteria().list{
			eq("paciente.id",idPaciente)
			order("fechaElaboracion","desc")
		}.each{ hoja->
		
		
			def soloLectura = hojaSoloLectura(hoja.fechaElaboracion)
			def botonTraslado=""	
			
			if(!soloLectura){
				botonTraslado = """				
				<td><input type="button" value="ACEPTAR" onclick="mostrarFirma('${hoja.id}',false,'Traslado','${hoja.fechaElaboracion.format('dd/MM/yyyy')}')"/></td>				
				"""
			}
			else{
				botonTraslado = "<td></td>"
			}
			
		
			html += """
				<tr>				
					<td>${hoja.fechaElaboracion.format('dd/MM/yyyy')}</td>
					<td>
						${hoja?.turnoMatutino?.jefe?"<a class=\"jefe\" title=\"Jef@:${hoja?.turnoMatutino?.jefe}\"><img src=\"/enfermeria/images/icons/seguridad.gif\"/></a>":''}
						${hoja?.turnoMatutino?.supervisor?"<a class=\"supervisor\" title=\"Supervis@r:${hoja?.turnoMatutino?.supervisor}\"><img src=\"/enfermeria/images/icons/usuarios.gif\"/></a>":''}						
						
						<ul style="margin:0;padding:0;list-style-type:none">
							<li><label style="color:blue">${hoja?.turnoMatutino?.usuario?:''}</label></li>
							<li><label style="color:red">${hoja?.turnoMatutino?.traslado1?:''}</label></li>
							<li><label style="color:red">${hoja?.turnoMatutino?.traslado2?:''}</label></li>
							<li><label style="color:red">${hoja?.turnoMatutino?.traslado3?:''}</label></li>
						</ul>
					</td>
					<td>
						${hoja?.turnoVespertino?.jefe?"<a class=\"jefe\" title=\"Jef@:${hoja?.turnoVespertino?.jefe}\"><img src=\"/enfermeria/images/icons/seguridad.gif\"/></a>":''}
						${hoja?.turnoVespertino?.supervisor?"<a class=\"supervisor\" title=\"Supervis@r:${hoja?.turnoVespertino?.supervisor}\"><img src=\"/enfermeria/images/icons/usuarios.gif\"/></a>":''}

						<ul style="margin:0;padding:0;list-style-type:none">
							<li><label style="color:blue">${hoja?.turnoVespertino?.usuario?:''}</label></li>
							<li><label style="color:red">${hoja?.turnoVespertino?.traslado1?:''}</label></li>
							<li><label style="color:red">${hoja?.turnoVespertino?.traslado2?:''}</label></li>
							<li><label style="color:red">${hoja?.turnoVespertino?.traslado3?:''}</label></li>
						</ul>				
					</td>
					<td>
						${hoja?.turnoNocturno?.jefe?"<a class=\"jefe\" title=\"Jef@:${hoja?.turnoNocturno?.jefe}\"><img src=\"/enfermeria/images/icons/seguridad.gif\"/></a>":''}
						${hoja?.turnoNocturno?.supervisor?"<a class=\"supervisor\" title=\"Supervis@r:${hoja?.turnoNocturno?.supervisor}\"><img src=\"/enfermeria/images/icons/usuarios.gif\"/></a>":''}

						<ul style="margin:0;padding:0;list-style-type:none">
							<li><label style="color:blue">${hoja?.turnoNocturno?.usuario?:''}</label></li>
							<li><label style="color:red">${hoja?.turnoNocturno?.traslado1?:''}</label></li>
							<li><label style="color:red">${hoja?.turnoNocturno?.traslado2?:''}</label></li>
							<li><label style="color:red">${hoja?.turnoNocturno?.traslado3?:''}</label></li>
						</ul>				
					</td>

					<td><input type="button" value="ACEPTAR" onclick="mostrarFirma('${hoja.id}',false,'Enfermera','${hoja.fechaElaboracion.format('dd/MM/yyyy')}')"/></td>

					${botonTraslado}				
					
				</tr>
			"""	
		
		}
		
		html += "</tbody></table></div>"
		
		html
		
	}

	def mostrarFirma(Long idHoja, boolean tieneUsuario=false, String tipoUsuario = "Enfermera"){
		
		def id = idHoja == 0?'':idHoja
		
		
		def usuario = ""
		
		
		if(tieneUsuario){
			
			usuario = """
				<tr>
					<td colspan="2"><label>Buscar usuario por RFC:</label></td>
				</tr>
				<tr>
					<td colspan="2">
						<input type="text" name="usuarioFirma" id="usuarioFirma" size="50"/>
						<input type="hidden" name="idUsuarioFirma" id="idUsuarioFirma"/>						
					</td>
				</tr>
			"""
		}
		
		def html = """

			<g:form>
				<input type="hidden" name="tipoUsuarioFirma" id="tipoUsuarioFirma" value="${tipoUsuario}"/>
				

				<table>			

				${usuario}

				<tr>
					<td colspan="2"><label>Password de firma:</label></td>
				</tr>
				<tr>
					<td colspan="2"><input type="password" name="passwordFirma" id="passwordFirma"/></td>
				<tr>
					<td><input type="button" id="btnFirmarHoja" onclick="firmarHoja('${id}')" value="Firmar Hoja"/></td>				
					<td><input type="button" onclick="jQuery('#mostrarFirma').dialog('close')" value="Cancelar"/></td>
				</tr>
				</table>
			</g:form>

		"""
		
		html
	}
	
	/***
	 * 
	 * Valida si existe la firma digital
	 * 
	 * @param idHoja
	 * @param turno
	 * @param idUsuario
	 * @param password
	 * @return si coincide
	 */
	def firmarHoja(Long idHoja,String asociarTurno, Usuario usuario, String password, jsonHoja,
		 Integer idUsuarioFirma = null, String tipoUsuario=null){
		
		boolean firmado = false	
		boolean nuevaHoja = false
		
		Usuario usuarioFirma
		
		if(['Jefe','Supervisor'].contains(tipoUsuario)){
			usuarioFirma = 	Usuario.get(idUsuarioFirma)
		}
		else{//Traslado
			usuarioFirma = usuario
		}
		
		
		FirmaDigital firmaDigital = FirmaDigital.findWhere(passwordfirma:password?.reverse(),
			id:usuarioFirma.id)
		
		if(firmaDigital){
			
			if(tipoUsuario != 'Enfermera'){//Firma jefe supervisor o traslado			
				
				def hojaTurno = HojaRegistroEnfermeriaTurno.createCriteria().get{
					eq("hoja.id",idHoja)
					eq("turno",Turno."${asociarTurno}")
					maxResults(1)
				}
				
				if(tipoUsuario == 'Traslado'){
				
					Integer numeroTraslado = 1;
					
					if(hojaTurno.traslado1){
						numeroTraslado=2
					}
					
					if(hojaTurno.traslado2){
						numeroTraslado=3
					}
					
					if(hojaTurno.traslado3){
						return [firmado:true,idHoja:idHoja]
					}
					
					tipoUsuario = "${tipoUsuario}${numeroTraslado}"
				}			
				
				
				hojaTurno."firma${tipoUsuario}"=true
				hojaTurno."${tipoUsuario.toLowerCase()}" = usuarioFirma
				hojaTurno."ip${tipoUsuario}"= utilService.getIpTerminal()
				hojaTurno."fecha${tipoUsuario}"=new Date()
				
				
				hojaTurno.save([validate:false])
			}
			else{
				jsonHoja.idHoja = idHoja //Si es una actualizacion
				jsonHoja.turno = asociarTurno
				HojaRegistroEnfermeria hoja= guardarHojaTurno(jsonHoja, usuarioFirma)
				idHoja = hoja.id
				
				nuevaHoja = isNuevaHoja(idHoja)
				
			}					
			
			firmado = true			
		}
		
		[firmado:firmado,idHoja:idHoja,nuevaHoja:nuevaHoja]
	}
	
    boolean isNuevaHoja(Long idHoja){
		def turnos = HojaRegistroEnfermeriaTurno.createCriteria().list {
			eq("hoja.id",idHoja)
		}
		
		turnos.size() == 1
		
	}
	
	boolean existeTurno(Long idHoja, String turno){
		
		def result = false
		
		def registroTurno = HojaRegistroEnfermeriaTurno.createCriteria().get{
			eq("hoja.id",idHoja)
			eq("turno",Turno."${turno}")
			maxResults(1)
		}
		
		if(registroTurno){
			result = true
		}
		
		result	
	}
	
	def existeHoja(Long idPaciente, Date fechaElaboracion){
		
		def existe = false
		
		def registro = HojaRegistroEnfermeria.createCriteria().get{
			eq("paciente.id",idPaciente)
			eq("fechaElaboracion",fechaElaboracion)
			maxResults(1)
		}
		
		if(registro){
			existe = true
		}
		
		[existe:existe,idHoja:registro?.id]
	}
	
	
	boolean existeFirma(Long idHoja,String tipoUsuario, String turnoActual){
		
		boolean result = false
		
		def turnoRegistro = HojaRegistroEnfermeriaTurno.createCriteria().get{
			eq("hoja.id",idHoja)
			eq("firma${tipoUsuario}",true)
			eq("turno",Turno."${turnoActual}")
			maxResults(1)			
		}
		
		
		if(turnoRegistro){
			result = true
		}
		
		result		
		
	}
	
	
	boolean duenoTurno(Long idHoja, String turno, Usuario usuario){		
		
		def result = false
		
		def registroTurno = HojaRegistroEnfermeriaTurno.createCriteria().get{
			eq("hoja.id",idHoja)
			eq("turno",Turno."${turno}")
			
			or{
				eq("usuario", usuario)
				eq("traslado1", usuario)
				eq("traslado2", usuario)
				eq("traslado3", usuario)
			}
			
			maxResults(1)
		}
		
		if(registroTurno){
			result = true
		}
		
		result		
	}
	
	
	/****
	 * Trae las hojas asociadas al usuario actual
	 * 
	 * @param idUsuario
	 * @return
	 */
	def misHojas(Long idUsuario, String turno){
		
		def html = """

			<label>Asociar turno:</label>
			<select name="turnoAsociar" id="turnoAsociar">
				<option value="NINGUNO" ${turno == 'NINGUNO'?'selected':''}>SELECCIONE TURNO</option>
				<option value="MATUTINO"  ${turno == 'MATUTINO'?'selected':''}>MATUTINO</option>
				<option value="VESPERTINO"  ${turno == 'VESPERTINO'?'selected':''}>VESPERTINO</option>
				<option value="NOCTURNO"  ${turno == 'NOCTURNO'?'selected':''}>NOCTURNO</option>
			</select>	
			
			<div style="height:500px;overflow:auto;" class="wrapper" >
			<table id="tablaHojas">
			<thead>			
					<tr>						
						<th>
							Fecha<br>Elaboracion
						</th>
						<th>
							Paciente
						</th>
						<th>
							Cama
						</th>
						<th>
							Abrir Hoja
						</th>						
					</tr>			
			</thead><tbody>

		"""
		
		if(turno != "NINGUNO"){
			HojaRegistroEnfermeria.createCriteria().list{
				
				turnos{
					
					or{
						eq("usuario.id", idUsuario)
						eq("traslado1.id", idUsuario)
						eq("traslado2.id", idUsuario)
						eq("traslado3.id", idUsuario)												
					}
					
					eq("turno",Turno."${turno}")
				}			
				
				admision{
					cama{
						order("numerocama","asc")
					}
				}
				
				paciente{
					
				}
				
				ge("fechaElaboracion", new Date())			
				
			}.each{ hoja->		
			
			
				html += """
					<tr>				
						<td>${hoja.fechaElaboracion.format('dd/MM/yyyy')}</td>
						<td>${hoja?.paciente}</td>
						<td>${hoja?.admision.cama}</td>
						<td><input type="button" value="ACEPTAR" onclick="mostrarFirma('${hoja.id}',false,'Enfermera','${hoja.fechaElaboracion.format('dd/MM/yyyy')}')"/></td>
					</tr>
				"""
			}
		}
		
		
		html += "</tbody></table></div>"
		
		html
		
	}	
	
}
