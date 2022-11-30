/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import React, {useContext} from 'react';
import { useResizeDetector } from 'react-resize-detector'
import Plot from 'react-plotly.js';
import Dialog from '@mui/material/Dialog';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import CloseIcon from '@mui/icons-material/Close';
import {Context} from '../store';
import {RunExt, PropertyValueType} from '../types';

function computeDifferentPropertyValues(runs: RunExt[]) {
  // Ignore a Duration property value if in all runs Duration was set by the resource
  const ignorePvs = runs.map((r) => r.propertyValueDurationViaResource).reduce((a, b) => a.filter(c => b.includes(c)));
  const values: {[s: string]: {name: string, type: PropertyValueType, runs: {[s: string]: string | number | null}}} = {};
  runs.forEach((run) => run.pvmt.functionalChains.forEach((fc) => fc.elements.forEach((element) => 
      element.propertyValueGroups.forEach((pvg) => pvg.propertyValues.forEach((pv) => {
    if (ignorePvs.includes(pv.id)) return;
    if (!values[pv.id]) values[pv.id] = {name: `${element.label || element.type}.${pv.label}`, type: pv.type, runs: {}};
    values[pv.id].runs[run.name] = pv.value;
  })))));

  return Object.values(values).filter((v) => new Set(Object.values(v.runs)).size != 1);
}

function ResizingPlot() {
  const {state} = useContext(Context);
  const {width, height, ref} = useResizeDetector({refreshMode: 'debounce', refreshRate: 100});
  const trace: any = {
    type: 'parcoords',
    line: {
      color: 'blue'
    },
    dimensions: [ 
      {
        label: 'run',
        values: state.runs.map((r, i) => i + 1),
        tickvals: state.runs.map((r, i) => i + 1),
        ticktext: state.runs.map((r) => r.name),
      },
      ...computeDifferentPropertyValues(state.runs).map((v) => {
        if (v.type.type === 'string' || v.type.type === 'enum') {
          const ticktext = Array.from(new Set(Object.values(v.runs)));
          return {label: v.name, ticktext, values: state.runs.map((r) => ticktext.indexOf(v.runs[r.name])), tickvals: ticktext.map((r, i) => i)};
        } else {
          return {label: v.name, values: state.runs.map((r) => v.runs[r.name])}
        }
      }),
      {
        label: 'total duration',
        values: state.runs.map((r, i) => r.time),
      },
    ],
  }

  return (
    <div ref={ref} style={{height: '100%', width: '100%', paddingTop: '10px'}}>
      <Plot
        style={{position: 'fixed'}}
        data={[trace]}
        layout={{width, height, yaxis: {title: 'Time (seconds)'}}}
      />
    </div>
  );
}

export default function RunsDialog() {
  const {state, dispatch} = useContext(Context);
  const close = () => dispatch({action: 'show_runs', value: false});
  return (
    <Dialog fullScreen open={state.showRuns} onClose={close}>
      <AppBar sx={{ position: 'relative' }}>
        <Toolbar>
          <IconButton edge="start" color="inherit" onClick={close}><CloseIcon /></IconButton>
          <Typography sx={{ ml: 2, flex: 1 }} variant="h6" component="div">
            Results of {state.runs.length} run(s)
          </Typography>
        </Toolbar>
      </AppBar>
      {state.showRuns === true && <ResizingPlot/>}
    </Dialog>
  );
}
