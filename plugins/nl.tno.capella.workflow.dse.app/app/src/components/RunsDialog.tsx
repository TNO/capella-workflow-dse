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
import { useResizeDetector } from 'react-resize-detector'
import Plot from 'react-plotly.js';
import Dialog from '@mui/material/Dialog';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import CloseIcon from '@mui/icons-material/Close';
import CompareArrowsIcon from '@mui/icons-material/CompareArrows';
import {Context, State} from '../store';
import {RunExt, PropertyValueType, Run} from '../types';

type GraphMode = 'overview' | 'cost_duration';

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

function getOverviewGraph(state: State, width: number, height: number) {
  const layout = {width, height, yaxis: {title: 'Time (seconds)'}};
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
      {
        label: 'total cost',
        values: state.runs.map((r, i) => r.totalCost),
      },
      {
        label: 'total cost used',
        values: state.runs.map((r, i) => r.totalCostUsed),
      },
    ],
  };
  return {data: [trace], layout};
}

function getCostDurationGraph(state: State, width: number, height: number) {
  const layout = {width, height, xaxis: {title: 'Time (seconds)'}, yaxis: {title: 'Total cost'}};
  const {runs} = state;
  const optimalRuns = runs.filter((a) => !runs.find((b) => a.name !== b.name && (!(b.totalCost == a.totalCost && b.time === a.time) && b.totalCost <= a.totalCost && b.time <= a.time)));
  const nonOptimalRuns = runs.filter((a) => !optimalRuns.find((b) => a.name === b.name));
  const trace = (runs: Run[], name: string, color: string, mode: string) => {
    const sorted = runs.sort((a, b) => a.totalCost - b.totalCost);
    return {name, mode, line: {color}, y: sorted.map((r) => r.totalCost), x: sorted.map((r) => r.time), text: sorted.map((r) => r.name)}
  };
  return {data: [trace(optimalRuns, 'Optimal runs', 'green', 'line'), trace(nonOptimalRuns, 'Non-optimal runs', 'blue', 'markers')], layout};
}

function ResizingPlot(props: {mode: GraphMode}) {
  const {state} = useContext(Context);
  const {width, height, ref} = useResizeDetector({refreshMode: 'debounce', refreshRate: 100});
  const graph = props.mode === 'overview' ? getOverviewGraph(state, width, height) : getCostDurationGraph(state, width, height);
  return (
    <div ref={ref} style={{height: '100%', width: '100%', paddingTop: '10px'}}>
      <Plot style={{position: 'fixed'}} data={graph.data} layout={graph.layout}/>
    </div>
  );
}

export default function RunsDialog() {
  const {state, dispatch} = useContext(Context);
  const [mode, setMode] = useState('overview' as GraphMode);
  const close = () => dispatch({action: 'show_runs', value: false});
  return (
    <Dialog fullScreen open={state.showRuns} onClose={close}>
      <AppBar sx={{ position: 'relative' }}>
        <Toolbar>
          <IconButton edge="start" color="inherit" onClick={close}><CloseIcon/></IconButton>
          <Typography sx={{ ml: 2, flex: 1 }} variant="h6" component="div">
            Results of {state.runs.length} run(s) ({mode === 'overview' ? 'overview' : 'total cost vs duration'} graph)
          </Typography>
          <IconButton edge="end" color="inherit" onClick={() => setMode(mode === 'overview' ? 'cost_duration' : 'overview')}>
            <CompareArrowsIcon/>
          </IconButton>
        </Toolbar>
      </AppBar>
      {state.showRuns === true && <ResizingPlot mode={mode}/>}
    </Dialog>
  );
}
