/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import React, {useContext, useState} from 'react';
import {Context} from '../store';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import WarningIcon from '@mui/icons-material/Warning';
import SaveIcon from '@mui/icons-material/Save';
import {DefinitionEntry} from '../types';
import {clone} from '../utils';
import {NumberInput, AutcompleteInput} from './Input';
import { ipcRenderer } from 'electron';
import Tooltip from '@mui/material/Tooltip';

function Item(props: {index: number, definition: DefinitionEntry, functions: string[], inFc: boolean}) {
  const {state, dispatch} = useContext(Context);

  const handleDelete = () => {
    const definitions = clone(state.definitions);
    definitions.splice(props.index, 1);
    dispatch({action: 'set_definitions', definitions});
  }

  const onChange = (type: 'duration' | 'resource' | 'function', value: string) => {
    const definitions = clone(state.definitions);
    definitions[props.index][type] = value;
    dispatch({action: 'set_definitions', definitions});
  }

  return (
    <TableRow>
      <TableCell>
        <div style={{display: 'flex', alignItems: 'center'}}>
          {!props.inFc && <Tooltip style={{marginRight: '10px'}}arrow title="Function is not included in functional chain"><WarningIcon/></Tooltip>}
          {props.inFc && <AutcompleteInput value={props.definition.function} options={props.functions} onChange={(v) => onChange('function', v)}/>}
          {!props.inFc && <div>{props.definition.function}</div>}
        </div>
      </TableCell>
      <TableCell>
        {props.inFc && <AutcompleteInput value={props.definition.resource} options={state.allResources} onChange={(v) => onChange('resource', v)} freeSolo/>}
        {!props.inFc && <div>{props.definition.resource}</div>}
        </TableCell>
      <TableCell>
        {props.inFc && <NumberInput type='float' value={props.definition.duration} onChange={(v) => onChange('duration', v)}/>}
        {!props.inFc && <div>{props.definition.duration}</div>}
      </TableCell>
      <TableCell><Button onClick={handleDelete}><DeleteIcon/></Button></TableCell>
    </TableRow>
  );
}

export default function DefinitionEditor(props: {open: boolean, onClose: () => void}) {
  const {state, dispatch} = useContext(Context);
  const [savedTooltip, setSavedTooltip] = useState(false);
  const functions = Array.from(new Set(state.pvmt.functionalChains.flatMap((fc) => fc.elements
    .filter(e => e.type == 'FunctionalChainInvolvementFunction' && e.propertyValueGroups.length).flatMap((e) => e.label))));
  const handleAdd = () => {
    const definitions = clone(state.definitions);
    definitions.push({function: '', duration: '1', resource: ''});
    dispatch({action: 'set_definitions', definitions});
  }
  
  const handleSave = () => {
    ipcRenderer.invoke('save-definitions', state.definitions);
    setSavedTooltip(true);
  }

  return (
    <Dialog open={props.open} onClose={props.onClose} fullWidth maxWidth='lg'>
      <DialogContent>
          <div style={{width: '100%', textAlign: 'right', display: 'flex', justifyContent: 'right'}}>
            <Tooltip title="Saved definitions!" leaveDelay={500} onClose={() => setSavedTooltip(false)} arrow open={savedTooltip}>
              <Button variant='outlined' onClick={handleSave}><SaveIcon/></Button>
            </Tooltip>
            <div style={{width: '20px'}}/>
            <Button variant='contained' onClick={handleAdd}><AddIcon/></Button>
          </div>
          <TableContainer>
            <Table aria-label="simple table">
              <TableHead>
                <TableRow>
                  <TableCell width='50%'>Function</TableCell>
                  <TableCell width='25%'>Resource</TableCell>
                  <TableCell width='15%'>Duration</TableCell>
                  <TableCell width='70px'/>
                </TableRow>
              </TableHead>
              <TableBody>
                {state.definitions.map((d, i) => {
                  const inFc = d.function === '' || functions.includes(d.function);
                  return [inFc, <Item key={i} index={i} definition={d} functions={functions} inFc={inFc}/>]
                }).sort((a, b) => Number(b[0]) - Number(a[0])).map((d) => d[1])}
              </TableBody>
            </Table>
          </TableContainer>
      </DialogContent>
    <DialogActions>
      <Button onClick={props.onClose}>Close</Button>
    </DialogActions>
  </Dialog>)
}