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
import {Context, getAllPVMTResources} from '../store';
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
import ErrorIcon from '@mui/icons-material/Error';
import SaveIcon from '@mui/icons-material/Save';
import {CostEntry, DurationEntry} from '../types';
import {clone} from '../utils';
import {NumberInput, AutcompleteInput} from './Input';
import { ipcRenderer } from 'electron';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';

function DurationItem(props: {index: number, duration: DurationEntry, functions: string[], inFc: boolean, isDuplicate: boolean}) {
  const {state, dispatch} = useContext(Context);

  const handleDelete = () => {
    const durations = clone(state.definitions.durations);
    durations.splice(props.index, 1);
    dispatch({action: 'set_definitions', definitions: {...state.definitions, durations}});
  }

  const onChange = (type: 'duration' | 'resource' | 'function', value: string) => {
    const durations = clone(state.definitions.durations);
    durations[props.index][type] = value;
    dispatch({action: 'set_definitions', definitions: {...state.definitions, durations}});
  }

  return (
    <TableRow>
      <TableCell>
        <div style={{display: 'flex', alignItems: 'center'}}>
          {!props.inFc && <Tooltip style={{marginRight: '10px'}}arrow title="Function is not included in functional chain"><WarningIcon/></Tooltip>}
          {props.isDuplicate && <Tooltip style={{marginRight: '10px'}}arrow title="Definition with same function and resource exists"><ErrorIcon/></Tooltip>}
          {props.inFc && <AutcompleteInput value={props.duration.function} options={props.functions} onChange={(v) => onChange('function', v)}/>}
          {!props.inFc && <div>{props.duration.function}</div>}
        </div>
      </TableCell>
      <TableCell>
        {props.inFc && <AutcompleteInput value={props.duration.resource} options={state.allResources} onChange={(v) => onChange('resource', v)} freeSolo/>}
        {!props.inFc && <div>{props.duration.resource}</div>}
      </TableCell>
      <TableCell>
        {props.inFc && <NumberInput type='float' value={props.duration.duration} onChange={(v) => onChange('duration', v)}/>}
        {!props.inFc && <div>{props.duration.duration}</div>}
      </TableCell>
      <TableCell><Button onClick={handleDelete}><DeleteIcon/></Button></TableCell>
    </TableRow>
  );
}

function DurationTable() {
  const {state, dispatch} = useContext(Context);
  const functions = Array.from(new Set(state.pvmt.functionalChains.flatMap((fc) => fc.elements
    .filter(e => e.type == 'FunctionalChainInvolvementFunction' && e.propertyValueGroups.length).flatMap((e) => e.label))));
  const handleAdd = () => {
    const durations = clone(state.definitions.durations);
    durations.push({function: '', duration: '1', resource: ''});
    dispatch({action: 'set_definitions', definitions: {...state.definitions, durations}});
  }

  return (
    <div>
      <Typography variant="h5" gutterBottom>Durations</Typography>
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
            {state.definitions.durations.map((d, i) => {
              const inFc = d.function === '' || functions.includes(d.function);
              const duplicates = state.definitions.durations.filter((dd) => dd.function === d.function && dd.resource == d.resource);
              return [inFc, <DurationItem key={i} index={i} duration={d} functions={functions} inFc={inFc} isDuplicate={duplicates.length > 1}/>]
            }).sort((a, b) => Number(b[0]) - Number(a[0])).map((d) => d[1])}
          </TableBody>
        </Table>
      </TableContainer>
      <div style={{marginTop: '10px', display: 'flex', justifyContent: 'right', width: '100%'}}>
        <Button variant='contained' onClick={handleAdd}><AddIcon/></Button>
      </div>
    </div>
  )
}

function CostItem(props: {index: number, cost: CostEntry, resources: string[], isDuplicate: boolean}) {
  const {state, dispatch} = useContext(Context);

  const handleDelete = () => {
    const costs = clone(state.definitions.costs);
    costs.splice(props.index, 1);
    dispatch({action: 'set_definitions', definitions: {...state.definitions, costs}});
  }

  const onChange = (type: 'resource' | 'cost', value: string) => {
    const costs = clone(state.definitions.costs);
    costs[props.index][type] = value;
    dispatch({action: 'set_definitions', definitions: {...state.definitions, costs}});
  }

  return (
    <TableRow>
      <TableCell>
        <div style={{display: 'flex', alignItems: 'center'}}>
          {props.isDuplicate && <Tooltip style={{marginRight: '10px'}}arrow title="Cost with same resource exists"><ErrorIcon/></Tooltip>}
          <AutcompleteInput value={props.cost.resource} options={props.resources} onChange={(v) => onChange('resource', v)} freeSolo/>
        </div>
      </TableCell>
      <TableCell>
        <NumberInput type='float' value={props.cost.cost} onChange={(v) => onChange('cost', v)}/>
      </TableCell>
      <TableCell><Button onClick={handleDelete}><DeleteIcon/></Button></TableCell>
    </TableRow>
  );
}

function CostTable() {
  const {state, dispatch} = useContext(Context);
  const handleAdd = () => {
    const costs = clone(state.definitions.costs);
    costs.push({resource: '', cost: ''});
    dispatch({action: 'set_definitions', definitions: {...state.definitions, costs}});
  }
  const missingResources = getAllPVMTResources(state.pvmt).filter((r) => r.trim() !== '' && !state.definitions.costs.find((c) => c.resource === r && c.cost.trim() != ''));

  return (
    <div>
      <Typography variant="h5" gutterBottom>Costs</Typography>
      <TableContainer>
        <Table aria-label="simple table">
          <TableHead>
            <TableRow>
              <TableCell width='70%'>Resource</TableCell>
              <TableCell width='20%'>Cost</TableCell>
              <TableCell width='70px'/>
            </TableRow>
          </TableHead>
          <TableBody>
            {state.definitions.costs.map((d, i) => {
              const duplicates = state.definitions.costs.filter((dd) => dd.resource === d.resource);
              return <CostItem key={i} index={i} cost={d} resources={state.allResources} isDuplicate={duplicates.length > 1}/>;
            })}
          </TableBody>
        </Table>
      </TableContainer>
      <div style={{marginTop: '10px', display: 'flex', justifyContent: 'space-between', width: '100%'}}>
        <Typography variant="body2">
          {missingResources.length > 0 && <i>No costs specified for used resources: {missingResources.join(', ')}, default of 0 will be used for these resources.</i>}
        </Typography>
        <Button variant='contained' onClick={handleAdd}><AddIcon/></Button>
      </div>
    </div>
  )
}

export default function DefinitionEditor(props: {open: boolean, onClose: () => void}) {
  const {state} = useContext(Context);
  const [savedTooltip, setSavedTooltip] = useState(false);
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
          </div>
          <DurationTable/>
          <CostTable/>
      </DialogContent>
    <DialogActions>
      <Button onClick={props.onClose}>Close</Button>
    </DialogActions>
  </Dialog>)
}