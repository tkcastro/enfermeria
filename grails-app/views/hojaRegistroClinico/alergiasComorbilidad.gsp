

<div class="nav operacion" role="navigation">
		<ul>
			<li>
				<a style="cursor:pointer" class="guardar" onclick="guardarHojaTurno()">GUARDAR / FIRMAR</a>
			</li>
		</ul>
</div>


<table>
	<tr>
		<td>
			<label for="comorbilidad">Comorbilidad:</label>
			<label for="has">HAS</label><g:checkBox name="has" value="${hojaInstance.has}"  />
			<label for="dm">DM</label><g:checkBox name="dm" value="${hojaInstance.dm}" />
			<label for="nef">NEF</label> <g:checkBox name="nef" value="${hojaInstance.nef}" />
			<label for="ic">IC</label> <g:checkBox name="ic" value="${hojaInstance.ic}" />
			<label for="ir">IR</label> <g:checkBox name="ir" value="${hojaInstance.ir}" />
		</td>
	</tr>
</table>

<table>
	<tr>

		<td>
			<table>
				<tr>
					<td>
						<label for="peso">Peso:</label>
						<g:textField name="peso" value="${hojaInstance.peso}" size="5" maxlength="6" class="numeroDecimal" />kg.				
					</td>
				</tr>
				<tr>
					<td>
						<label for="talla">Talla:</label>
						<g:textField name="talla" value="${hojaInstance.talla}" size="4" maxlength="4" class="numeroDecimal" />m.
					</td>
				</tr>

			</table>
		</td>



		<td><label for="alergias">Alergias:</label> <g:textField
				name="alergias" value="${hojaInstance.alergias}" maxlength="250"  /></td>

		<td><label for="otro">Otro:</label> <g:textField name="otros"
				value="${hojaInstance.otros}" maxlength="50"  /></td>
	</tr>

</table>
