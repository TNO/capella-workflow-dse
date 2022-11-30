/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import React from 'react';
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';
import IconButton from '@mui/material/IconButton';
import AddIcon from '@mui/icons-material/Add';
import CloseIcon from '@mui/icons-material/Close';

export function NumberInput(props: {value: string, type: 'int' | 'float', onChange: (value: string) => void, label?: string}) {
  return (
    <TextField 
      size='small'
      label={props.label}
      style={{width: '100%'}} 
      InputLabelProps={{shrink: true}}
      type='number'
      value={props.value || ''} 
      onClick={(e) => e.stopPropagation()}
      onChange={(e) => {
        const re = props.type === 'int' ? /^[0-9]*$/ : /^[0-9]*\.?[0-9]*$/;
        re.test(e.target.value) && props.onChange(e.target.value)
      }} 
    />
  )
}

export function AutcompleteInput(props: {value: string, options: string[], onChange: (value: string) => void, freeSolo?: boolean}) {
  return (
    <Autocomplete
      value={props.value || null}
      size='small'
      freeSolo={props.freeSolo}
      style={{width: '100%'}}
      onInputChange={(e, v) => props.freeSolo && v != null && props.onChange(v)}
      onChange={(e, v) => props.onChange(v || '')}
      options={props.options}
      renderInput={(params) => <TextField {...params}/>}    
    /> 
  );
}

export function TextInput(props: {value: string, onChange: (value: string) => void}) {
  return (
    <TextField size='small' style={{width: '100%'}} value={props.value || ''} onChange={(e) => props.onChange(e.target.value || '')}/>
  )
}

export function ListInput(props: {child: React.ReactElement, index: number, onDelete: (i: number) => void, onAdd: () => void}) {
  const handleClick = (evt: React.MouseEvent) => {
    evt.stopPropagation();
    props.index === 0 ? props.onAdd() : props.onDelete(props.index);
  }

  return (
    <div style={{display: 'flex', width: '100%', marginTop: props.index !== 0 ? '5px' : '0px'}}>
      {props.child}
      <IconButton aria-label="delete" style={{marginLeft: '5px'}} onClick={handleClick}>
        {props.index === 0 ? <AddIcon/> : <CloseIcon/>}
      </IconButton>
    </div>
  );
}
