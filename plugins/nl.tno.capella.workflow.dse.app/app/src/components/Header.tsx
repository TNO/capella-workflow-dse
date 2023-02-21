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
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { styled } from '@mui/material/styles';
import Alert from '@mui/material/Alert';
import Snackbar from '@mui/material/Snackbar';
import TextField from '@mui/material/TextField';
import ButtonUnstyled from '@mui/material/Button';
import ButtonGroup from '@mui/material/ButtonGroup';
import ClickAwayListener from '@mui/material/ClickAwayListener';
import Grow from '@mui/material/Grow';
import Paper from '@mui/material/Paper';
import Popper from '@mui/material/Popper';
import MenuItem from '@mui/material/MenuItem';
import MenuList from '@mui/material/MenuList';
import {run} from '../runner';
import InputAdornment from '@mui/material/InputAdornment';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import StackedLineChartIcon from '@mui/icons-material/StackedLineChart';
import FormatListBulletedIcon from '@mui/icons-material/FormatListBulleted';
import ClearIcon from '@mui/icons-material/Clear';
import StopIcon from '@mui/icons-material/Stop';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DefinitionEditor from './DefinitionEditor';
import { ipcRenderer } from 'electron';
import CircularProgress from '@mui/material/CircularProgress';
import Tooltip from '@mui/material/Tooltip';
import { RunMode } from '../types';

const RunsTextField = styled(TextField)({
  '& .MuiOutlinedInput-root': {
    background: 'white',
    '&:hover': {backgroundColor: 'white'},
    '&.Mui-focused': {backgroundColor: 'white'},
  }
});

const Button = styled(ButtonUnstyled)({
  backgroundColor: 'white',
  borderColor: 'lightgrey !important',
  color: 'black',
  '&:hover': {
    backgroundColor: 'lightgrey',
  },
  '&:disabled': {
    backgroundColor: 'lightgrey',
  },
});

type Severity = 'error' | 'info' | 'success';
const progressSeverityLookup: {[s: string] : Severity} = {'error': 'error', 'done': 'success', 'in_progress': 'info'};
const initialSnackbar: {open: boolean, message: string, severity: Severity} = {open: false, message: '', severity: 'success'};

function ClearRunsConfirmDialog(props: {open: boolean, onClose: () => void, onConfirm: () => void, runCount: number}) {
  return (
    <Dialog open={props.open} onClose={props.onClose}>
    <DialogContent>
      <DialogContentText>
        Do you want to remove {props.runCount} run(s)?
      </DialogContentText>
    </DialogContent>
    <DialogActions>
      <ButtonUnstyled onClick={props.onClose}>Cancel</ButtonUnstyled>
      <ButtonUnstyled onClick={props.onConfirm} autoFocus>Confirm</ButtonUnstyled>
    </DialogActions>
  </Dialog>)
}

export default function Header() {
  const [running, setRunning] = useState(false);
  const [runModeOpen, setRunModeOpen] = useState(false);
  const [runMode, setRunMode] = useState('python' as RunMode);
  const [showDefinitionEditor, setShowDefinitionEditor] = useState(false);
  const [runsPerCombo, setRunsPerCombo] = useState(1);
  const [openClearRunsConfirmDialog, setOpenClearRunsConfirmDialog] = useState(false);
  const [snackbar, setSnackbar] = useState(initialSnackbar);
  const {state, dispatch} = useContext(Context);

  const anchorRef = React.useRef<HTMLDivElement>(null);
  const runModeOptions: {key: RunMode, title: string}[] = [
    {key: 'python', title: 'Run with Python (SNAKES)'}, {key: 'poosl', title: 'Run with POOSL'}, 
  ];

  const handleRunsTextFieldChanged = (e: React.ChangeEvent<HTMLInputElement>) => setRunsPerCombo(Math.max(1, Number(e.target.value)));

  const handleRunStopClick = async () => {
    if (running) {
      ipcRenderer.invoke('cancel-run');
    } else {
      setRunning(true);
      await run(state.pvmt, runsPerCombo, runMode, dispatch, (progress) => {
        setSnackbar({open: true, severity: progressSeverityLookup[progress.state], message: progress.message});
      });
      setRunning(false);
    }
  }

  const handleClearRuns = async () => {
    await ipcRenderer.invoke('clear-runs');
    await dispatch({action: 'clear_runs'});
    setOpenClearRunsConfirmDialog(false);
  }

  const runsPerComboDisabled = !state.pvmt.functionalChains.find((fc) => fc.elements.find((e) => e.controlNodeKind === 'OR'));

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <DefinitionEditor open={showDefinitionEditor} onClose={() => setShowDefinitionEditor(false)}/> 
        <ClearRunsConfirmDialog 
          runCount={state.runs.length} 
          open={openClearRunsConfirmDialog} 
          onClose={() => setOpenClearRunsConfirmDialog(false)} 
          onConfirm={handleClearRuns}
        />
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            {state.pvmt.name}
          </Typography>
          <Tooltip title="Definition editor" arrow>
            <Button variant='contained' onClick={() => setShowDefinitionEditor(true)}>
              <FormatListBulletedIcon />
            </Button>
          </Tooltip>
          <div style={{width: '5px'}}/>
          <Tooltip title="Clear run(s)" arrow>
            <span>
              <Button disabled={!state.runs.length} variant='contained' onClick={() => setOpenClearRunsConfirmDialog(true)}>
                <ClearIcon />
              </Button>
            </span>
          </Tooltip>
          <div style={{width: '5px'}}/>
          <Tooltip title="Run(s) graph" arrow>
            <span>
              <Button disabled={!state.runs.length} variant='contained' onClick={() => dispatch({action: 'show_runs', value: true})}>
                <StackedLineChartIcon />
              </Button>
            </span>
          </Tooltip>
          <div style={{width: '20px'}}/>
          <Tooltip title={runsPerComboDisabled ? 'Disabled because there are no OR nodes' : ''} arrow>
            <RunsTextField 
              InputProps={{startAdornment: <InputAdornment position="start">Runs per combo</InputAdornment>}} 
              size='small'
              disabled={runsPerComboDisabled}
              value={runsPerCombo} 
              type='number' 
              variant='outlined' 
              style={{width: '195px'}} 
              onChange={handleRunsTextFieldChanged}/>
          </Tooltip>
          <div style={{width: '5px'}}/>
          <ButtonGroup variant="contained" ref={anchorRef} aria-label="split button">
            <Tooltip title={running ? 'Stop simulation' : 'Run simulation'} arrow>
              <Button onClick={handleRunStopClick} variant="contained" className={running ? "running" : ""}>
                {running ? <StopIcon/> : <PlayArrowIcon/>}
              </Button>
            </Tooltip>
            <Button size="small" disabled={running} onClick={() => setRunModeOpen(!runModeOpen)}>
              <ArrowDropDownIcon />
            </Button>
          </ButtonGroup>
          <Popper sx={{zIndex: 1}} open={runModeOpen} anchorEl={anchorRef.current} role={undefined} transition disablePortal>
            {({ TransitionProps, placement }) => (
              <Grow {...TransitionProps} style={{transformOrigin: placement === 'bottom' ? 'center top' : 'center bottom'}}>
                <Paper>
                  <ClickAwayListener onClickAway={() => setRunModeOpen(false)}>
                    <MenuList autoFocusItem>
                      {runModeOptions.map((option, index) => (
                        <MenuItem
                          key={option.key}
                          selected={option.key === runMode}
                          onClick={(event) => { setRunModeOpen(false); setRunMode(option.key)}}
                        >
                          {option.title}
                        </MenuItem>
                      ))}
                    </MenuList>
                  </ClickAwayListener>
                </Paper>
              </Grow>
            )}
          </Popper>
        </Toolbar>
      </AppBar>
      <Snackbar
        open={snackbar.open}
        style={{top: '80px'}}
        anchorOrigin={{horizontal: 'right', vertical: 'top'}}
      >
        <Alert 
          onClose={running ? null : () => setSnackbar({...snackbar, open: false})} 
          severity={snackbar.severity} 
          sx={{ width: '100%' }}
          icon={running ? false : null}
        >
          <div style={{display: 'flex'}}>
            {running && 
              <div style={{marginRight: '10px', marginTop: '2px', height: '1px'}}>
                <CircularProgress size='1rem' color='inherit'/>
              </div>
            }
            {snackbar.message}
          </div>
        </Alert>
      </Snackbar>
    </Box>
  );
}