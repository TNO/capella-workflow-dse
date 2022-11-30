/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import React, { useContext, useEffect, useState } from 'react';
import {
  isRange, PropertyValueExtString, ElementExt, PropertyValueGroupExt, Range, PropertyValueExtNumber, isPropertyValueExtEnum,
  isPropertyValueExtNumber, PropertyValueNumber, PropertyValueExt, PropertyValueExtEnum, isPropertyValueExtString,
  isPropertyValueExtDurationViaResource, PVMTExport,
  PropertyValueExtDurationViaResource} from '../types';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import {Context} from '../store';
import colors from '../colors';
import IconButton from '@mui/material/IconButton';
import TuneIcon from '@mui/icons-material/Tune';
import AdjustIcon from '@mui/icons-material/Adjust';
import {NumberInput, AutcompleteInput, TextInput, ListInput} from './Input';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import UploadIcon from '@mui/icons-material/Upload';
import DownloadIcon from '@mui/icons-material/Download';
import Tooltip from '@mui/material/Tooltip';
import { ipcRenderer } from 'electron';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';

const style = {
  pvg: {fontSize: '15px', marginLeft: '5px'},
  pv: {fontSize: '15px', marginLeft: '15px'},
}

const jsonFileFilters = [{name: 'JSON Files', extensions: ['json']}];

function PropertyValueNumber(props: {pv: PropertyValueExtNumber}) {
  const {dispatch} = useContext(Context);
  const {pv} = props;
  const setValue = (value: string | Range, key?: 'min'|'max'|'step') => {
    if (key && isRange(pv.value)) {
      dispatch({action: 'update_property_value', pv, value: {...pv.value, [key]: value}})
    } else {
      dispatch({action: 'update_property_value', pv, value})
    }
  };
  const handleIconButtonClick = (evt: React.MouseEvent) => {
    evt.stopPropagation();
    if (isRange(pv.value)) {
      setValue(pv.value.min);
    } else {
      setValue({min: pv.value, max: pv.value, step: '1'});
    }
  }

  return (
    <div style={{display: 'flex'}}>
      { isRange(pv.value) ?
        (
          <div style={{display: 'flex'}}>
            <NumberInput onChange={(v) => setValue(v, 'min')} type={pv.type.type} value={pv.value.min} label='min'/>
            <div style={{width: '5px'}}/>
            <NumberInput onChange={(v) => setValue(v, 'max')} type={pv.type.type} value={pv.value.max} label='max'/>
            <div style={{width: '5px'}}/>
            <NumberInput onChange={(v) => setValue(v, 'step')} type={pv.type.type} value={pv.value.step} label='step'/>
          </div>
        ) :
        (
          <NumberInput onChange={setValue} type={pv.type.type} value={pv.value}/>
        )
      }
      <IconButton style={{marginLeft: '5px'}} onClick={handleIconButtonClick}>
        {isRange(pv.value) ? <TuneIcon/> : <AdjustIcon/>}
      </IconButton>
    </div>
  );
}

function PropertyValueStringEnum(props: {pv: PropertyValueExtString | PropertyValueExtEnum}) {
  const {pv} = props;
  const {dispatch, state} = useContext(Context);

  const onDelete = (index: number) => {
    let value = [...pv.value];
    value.splice(index, 1);
    dispatch({action: 'update_property_value', pv, value})
  }
  const onAdd = () => dispatch({action: 'update_property_value', pv, value: [...pv.value, '']});
  const onChange = (index: number, v: string) => {
    let value = [...pv.value];
    value[index] = v;
    dispatch({action: 'update_property_value', pv, value})
  }

  let childs: React.ReactElement[] = [];
  if (pv.label === 'ResourceID') {
    childs = pv.value.map((v, i) => <AutcompleteInput freeSolo value={pv.value[i]} options={state.allResources} onChange={(v) => onChange(i, v)}/>)
  } else if (isPropertyValueExtEnum(pv)) {
    childs = pv.value.map((v, i) => <AutcompleteInput value={pv.value[i]} options={pv.type.values} onChange={(v) => onChange(i, v)}/>)
  } else {
    childs = pv.value.map((v, i) => <TextInput value={pv.value[i]} onChange={(v) => onChange(i, v)}/>)
  }

  return (
    <div>
      {childs.map((child, i) => <ListInput key={i} child={child} index={i} onAdd={onAdd} onDelete={onDelete}/>)}
    </div>
  );
}

function PropertyValueDurationViaResource(props: {pv: PropertyValueExtDurationViaResource}) {
  return (
    <ul style={{paddingLeft: '20px'}}>
      {Object.entries(props.pv.value).map((e, i) => {
        return <li key={i}>{e[1].value}{e[1].default ? ' (default)' : ''} when resource is "{e[0]}"</li>
      })}
    </ul>
  )
} 

function PropertyValue(props: {pv: PropertyValueExt}) {
  const {pv} = props;
  return (
    <tr>
      <td>{props.pv.label}</td>
      <td style={{width: '100%', paddingTop: '5px'}}>
        {isPropertyValueExtNumber(pv) && <PropertyValueNumber pv={pv}/>}
        {isPropertyValueExtString(pv) && <PropertyValueStringEnum pv={pv}/>}
        {isPropertyValueExtDurationViaResource(pv) && <PropertyValueDurationViaResource pv={pv}/>}
      </td>
    </tr>
  );
}

function PropertyValueGroup(props: {pvg: PropertyValueGroupExt}) {
  return (
    <div style={style.pvg}>
      - {props.pvg.label}
      <table style={style.pv}>
        <tbody>
          {props.pvg.propertyValues.map((pv, i) => <PropertyValue key={i} pv={pv}/>)}
        </tbody>
      </table>
    </div>
  );
}

function LoadPVMTDialog(props: {open: boolean, onClose: () => void, title: string, content: string}) {
  return (
    <Dialog open={props.open} onClose={props.onClose}>
      <DialogTitle>{props.title}</DialogTitle>
      <DialogContent>
        {props.content.split('\n').map((l, i) => <DialogContentText key={i}>{l}</DialogContentText>)}
      </DialogContent>
      <DialogActions>
        <Button onClick={props.onClose} autoFocus>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

function Element(props: {element: ElementExt, selected: boolean, highlighted: boolean, isLast: boolean}) {
  const borderColor = props.selected ? colors.selected : props.highlighted ? colors.highlighted : 'white';
  const {dispatch} = useContext(Context);
  return (
    <ListItem divider={!props.isLast}>
      <ListItemText 
        id={`props-${props.element.id}`} 
        style={{border: `3px solid ${borderColor}`, borderRadius: '5px'}} 
        onMouseOver={() => dispatch({action: 'set_highlighted_element', id: props.element.id})}
        onMouseOut={() => dispatch({action: 'set_highlighted_element', id: null})}
        onClick={() => dispatch({action: 'set_selected_element', id: props.element.id})}
      >
        {props.element.label || props.element.type}<br/>
        {props.element.propertyValueGroups.map((pvg, i) => <PropertyValueGroup key={i} pvg={pvg}/>)}
        {props.element.propertyValueGroups.length == 0 && 
          <div style={style.pvg}>No property value groups attached</div>
        }
      </ListItemText>
    </ListItem>
  );
}

export default function PVMTEditor() {
  const {state, dispatch} = useContext(Context);
  const elements = state.pvmt.functionalChains.flatMap((fc) => fc.elements).slice()
    .sort((a, b) => b.propertyValueGroups.length - a.propertyValueGroups.length);
  const [dialog, setDialog] = useState({open: false, title: '', content: ''});

  useEffect(() => {
    if (state.selectedElement) {
      document.getElementById(`props-${state.selectedElement}`).scrollIntoView({behavior: 'smooth'});
    }
  }, [state.selectedElement]);

  const propertyValues = state.pvmt.functionalChains.flatMap((fc) => fc.elements
    .flatMap((e) => e.propertyValueGroups.flatMap((pvg) => pvg.propertyValues)));

  const savePVMT = () => {
    const pvmtExport: PVMTExport = propertyValues
      .filter((pv) => !isPropertyValueExtDurationViaResource(pv))
      .map((pv) => pv as PropertyValueExtString | PropertyValueExtNumber | PropertyValueExtEnum);
    const filename = `PVMT-${state.pvmt.name}.json`;
    ipcRenderer.invoke('save-file', 'Save PVMT', jsonFileFilters, filename , JSON.stringify(pvmtExport, null, 2));
  }

  const loadPVMT = async () => {
    const data = await ipcRenderer.invoke('load-file', 'Load PVMT', jsonFileFilters);
    if (data) {
      try {
        let warnings = [];
        const pvmtExport: PVMTExport = JSON.parse(data);
        for (const pv of pvmtExport) {
          const existingPV = propertyValues.find((p) => p.id === pv.id);
          if (!existingPV) {
            warnings.push(`- PropertyValue '${pv.label}' (${pv.id}) does not exist, skipped`);
          } else if (isPropertyValueExtDurationViaResource(existingPV)) {
            warnings.push(`- PropertyValue '${pv.label}' (${pv.id}) is set by resource, skipped`);
          } else if (existingPV.type.type !== pv.type.type) {
            warnings.push(`- PropertyValue '${pv.label}' (${pv.id}) type mismatch, ` +
              `expected '${existingPV.type.type}', got '${pv.type.type}', skipped`);
          } else {
            dispatch({action: 'update_property_value', pv, value: pv.value})
          }
        }
        if (warnings.length) {
          setDialog({open: true, title: 'Warning(s)', content: `Warning(s) while loading PVMT values:\n${warnings.join('\n')}`});
        } else {
          setDialog({open: true, title: 'Success', content: `Successfully imported PVMT values`});
        }
      } catch (e) {
        setDialog({open: true, title: 'Error', content: `Failed to load PVMT values: ${e.message}`});
      }
    }
  }

  const style = {
    width: '100%',
    height: '100%',
    overflowY: 'scroll',
  };
  return (
    <div style={{height: '100%', display: 'flex', flexFlow: 'column' }}>
      <LoadPVMTDialog 
        open={dialog.open} 
        title={dialog.title} 
        content={dialog.content} 
        onClose={() => setDialog({...dialog, open: false})}
      />
      <AppBar style={{backgroundColor: '#d3d3d3'}} position="static">
        <Toolbar>
          <div style={{display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center'}}>
            <Typography variant="h6" component="div" sx={{color: '#4a4a4a'}}>PVMT</Typography>
            <div>
              <Tooltip title="Save PVMT values" arrow><IconButton onClick={savePVMT}><UploadIcon/></IconButton></Tooltip>
              <Tooltip title="Load PVMT values" arrow><IconButton onClick={loadPVMT}><DownloadIcon/></IconButton></Tooltip>
            </div>
          </div>
        </Toolbar>
      </AppBar>
      <List sx={style}>
        {elements.map((d, i) => (
          <Element 
            isLast={i === (elements.length - 1)} 
            key={i} 
            element={d} 
            selected={state.selectedElement === d.id} 
            highlighted={state.highlightedElement === d.id}/>
          ))}
      </List>
    </div>
  );
}
